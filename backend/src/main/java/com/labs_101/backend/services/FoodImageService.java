package com.labs_101.backend.services;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.labs_101.backend.dtos.foodImage.AcceptFoodImageDto;
import com.labs_101.backend.dtos.foodImage.FoodImageAnalysisDto;
import com.labs_101.backend.dtos.foodImage.FoodImageItemDto;
import com.labs_101.backend.dtos.foodImage.FoodImageUsageDto;
import com.labs_101.backend.dtos.notification.CreateNotificationDto;
import com.labs_101.backend.entities.NotificationType;
import com.labs_101.backend.entities.User;
import com.labs_101.backend.entities.food.Food;
import com.labs_101.backend.entities.food.FoodImage;
import com.labs_101.backend.entities.food.FoodImageAnalysis;
import com.labs_101.backend.entities.food.FoodImageAnalysis.Item;
import com.labs_101.backend.entities.food.FoodImageStatus;
import com.labs_101.backend.entities.food.FoodPortion;
import com.labs_101.backend.entities.food.MealType;
import com.labs_101.backend.entities.food.TrackedFood;
import com.labs_101.backend.exception.BadRequestException;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.exception.TooManyRequestsException;
import com.labs_101.backend.foodExtractor.FoodExtractor;
import com.labs_101.backend.foodImage.GeminiClient;
import com.labs_101.backend.foodImage.GeminiClient.DetectedFood;
import com.labs_101.backend.mapper.FoodMapper;
import com.labs_101.backend.repositories.FoodImageAnalysisRepository;
import com.labs_101.backend.repositories.FoodImageRepository;
import com.labs_101.backend.repositories.FoodRepository;
import com.labs_101.backend.repositories.OpenFoodRepository;
import com.labs_101.backend.repositories.TrackedFoodRepository;
import com.labs_101.backend.repositories.UserRepository;

/**
 * Photos of meals: the user uploads one, {@code FoodImageJob} lets Gemini
 * analyze it in the background right after the upload and notifies the user,
 * who then checks the foods and accepts (tracks them) or rejects them. The
 * photo is kept either way.
 */
@Service
public class FoodImageService {
    static final int MAX_ATTEMPTS = 3;
    static final long MAX_BYTES = 10 * 1024 * 1024;
    // the formats Gemini accepts
    static final Set<String> CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/heic",
            "image/heif");
    static final Set<String> GRAM_UNITS = Set.of("g", "ml");
    // fallback if the model didn't estimate the grams of a unit
    static final double DEFAULT_GRAMS_PER_UNIT = 100;
    private static final int MAX_ERROR_LENGTH = 1000;
    static final int MAX_DESCRIPTION_LENGTH = 500;

    /** Published on upload, the job starts once the photo is committed. */
    public record UploadedEvent(Long id) {
    }

    private final Logger logger = LoggerFactory.getLogger(FoodImageService.class);

    private final FoodImageAnalysisRepository analysisRepository;
    private final FoodImageRepository imageRepository;
    private final UserRepository userRepository;
    private final FoodRepository foodRepository;
    private final OpenFoodRepository openFoodRepository;
    private final TrackedFoodRepository trackedFoodRepository;
    private final FoodService foodService;
    private final FoodExtractor foodExtractor;
    private final FoodMapper foodMapper;
    private final GeminiClient geminiClient;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final int dailyLimit;

    FoodImageService(FoodImageAnalysisRepository analysisRepository, FoodImageRepository imageRepository,
            UserRepository userRepository, FoodRepository foodRepository, OpenFoodRepository openFoodRepository,
            TrackedFoodRepository trackedFoodRepository, FoodService foodService, FoodExtractor foodExtractor,
            FoodMapper foodMapper, GeminiClient geminiClient, NotificationService notificationService,
            TransactionTemplate transactionTemplate, ApplicationEventPublisher eventPublisher,
            @Value("${food-image.daily-limit:5}") int dailyLimit) {
        this.analysisRepository = analysisRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.foodRepository = foodRepository;
        this.openFoodRepository = openFoodRepository;
        this.trackedFoodRepository = trackedFoodRepository;
        this.foodService = foodService;
        this.foodExtractor = foodExtractor;
        this.foodMapper = foodMapper;
        this.geminiClient = geminiClient;
        this.notificationService = notificationService;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
        this.dailyLimit = dailyLimit;
    }

    /**
     * Stores the photo, the job analyzes it once the transaction is committed.
     *
     * @param description optional, what the user says is on the photo, it is
     *                    passed to the model as hint
     */
    @Transactional
    public FoodImageAnalysisDto upload(String userId, MultipartFile file, String meal, String description) {
        if (!geminiClient.isConfigured())
            throw BadRequestException.foodImageDisabled();
        MealType mealType = parseMeal(meal);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!CONTENT_TYPES.contains(contentType))
            throw BadRequestException.foodImageType(contentType);
        if (file.isEmpty() || file.getSize() > MAX_BYTES)
            throw BadRequestException.foodImageSize(MAX_BYTES / 1024 / 1024);
        String hint = description == null || description.isBlank() ? null : description.strip();
        if (hint != null && hint.length() > MAX_DESCRIPTION_LENGTH)
            throw BadRequestException.foodImageDescription(MAX_DESCRIPTION_LENGTH);

        // two uploads at the same time would both pass the limit otherwise
        User user = userRepository.lockById(userId).orElseThrow(() -> NotFoundException.user(userId));
        if (countToday(userId) >= dailyLimit)
            throw TooManyRequestsException.foodImageLimit(dailyLimit);

        FoodImageAnalysis analysis = new FoodImageAnalysis();
        analysis.setUser(user);
        analysis.setMeal(mealType);
        analysis.setDescription(hint);
        analysis.setCreatedAt(Instant.now());
        analysis.setNextAttemptAt(analysis.getCreatedAt());
        analysisRepository.save(analysis);
        try {
            imageRepository.save(new FoodImage(analysis, contentType, file.getBytes()));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not read the uploaded photo", e);
        }
        logger.info("Photo {} of user {} was uploaded ({} bytes)", analysis.getId(), userId, file.getSize());
        eventPublisher.publishEvent(new UploadedEvent(analysis.getId()));
        return toDto(analysis);
    }

    public FoodImageUsageDto usage(String userId) {
        return new FoodImageUsageDto(countToday(userId), dailyLimit);
    }

    /** Newest first. */
    @Transactional(readOnly = true)
    public List<FoodImageAnalysisDto> getAll(String userId, int limit) {
        return analysisRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FoodImageAnalysisDto get(String userId, Long id) {
        return toDto(findAnalysis(userId, id));
    }

    @Transactional(readOnly = true)
    public FoodImage getImage(String userId, Long id) {
        findAnalysis(userId, id);
        return imageRepository.findById(id).orElseThrow(() -> NotFoundException.foodImage(id));
    }

    /**
     * Tracks the foods as checked by the user. Open foods are imported and
     * missing units added as portion, like when tracking them by text.
     */
    @Transactional
    public FoodImageAnalysisDto accept(String userId, Long id, AcceptFoodImageDto dto) {
        FoodImageAnalysis analysis = findAnalysis(userId, id);
        if (analysis.getStatus() != FoodImageStatus.READY)
            throw BadRequestException.foodImageNotReady(id);
        MealType meal = dto.meal() == null ? analysis.getMeal() : parseMeal(dto.meal());
        List<AcceptFoodImageDto.Item> items = dto.items() == null ? List.of() : dto.items();

        List<Item> accepted = new ArrayList<>();
        for (AcceptFoodImageDto.Item item : items) {
            if (item.amount() == null || item.amount() <= 0 || (item.foodId() == null && item.openFoodId() == null))
                throw BadRequestException.foodImageItem();
            Food food = item.foodId() != null
                    ? foodRepository.findById(item.foodId()).orElseThrow(() -> NotFoundException.food(item.foodId()))
                    : foodService.importOpenFood(openFoodRepository.findById(item.openFoodId())
                            .orElseThrow(() -> NotFoundException.openFood(item.openFoodId())));

            String unit = normalizeUnit(item.unit());
            double gramsPerUnit = gramsPerUnit(unit, item.gramsPerUnit());
            FoodPortion portion = GRAM_UNITS.contains(unit) ? null : findOrCreatePortion(food, unit, gramsPerUnit);
            // without a portion the amount is in grams (ml are counted as grams)
            double amount = portion == null ? item.amount() * gramsPerUnit : item.amount();

            trackedFoodRepository.save(new TrackedFood(null, analysis.getUser(), food, amount, meal, portion, null,
                    null));
            accepted.add(new Item(food.getName(), item.amount(), unit,
                    portion == null ? gramsPerUnit : portion.getGrams(), food.getId(), null));
        }

        analysis.setItems(accepted);
        analysis.setMeal(meal);
        analysis.setStatus(FoodImageStatus.ACCEPTED);
        analysis.setReviewedAt(Instant.now());
        logger.info("Photo {} was accepted with {} foods", id, accepted.size());
        return toDto(analysisRepository.save(analysis));
    }

    @Transactional
    public FoodImageAnalysisDto reject(String userId, Long id) {
        FoodImageAnalysis analysis = findAnalysis(userId, id);
        if (analysis.getStatus() != FoodImageStatus.READY)
            throw BadRequestException.foodImageNotReady(id);
        analysis.setStatus(FoodImageStatus.REJECTED);
        analysis.setReviewedAt(Instant.now());
        return toDto(analysisRepository.save(analysis));
    }

    // ---- used by the job ----

    /** Pending photos and when to try them, e.g. to continue after a restart. */
    public List<FoodImageAnalysisRepository.Pending> findPending() {
        return analysisRepository.findByStatusOrderByNextAttemptAt(FoodImageStatus.PENDING);
    }

    /**
     * Lets Gemini analyze the photo and matches the foods against our
     * databases. No transaction while waiting for Gemini, that takes seconds.
     *
     * @return when to try again after a failed attempt, empty when done
     */
    public Optional<Instant> analyze(Long id) {
        Optional<FoodImageAnalysis> analysis = analysisRepository.findById(id);
        Optional<FoodImage> image = imageRepository.findById(id);
        if (analysis.isEmpty() || image.isEmpty() || analysis.get().getStatus() != FoodImageStatus.PENDING)
            return Optional.empty();

        List<DetectedFood> detected;
        try {
            detected = geminiClient.detect(image.get().getData(), image.get().getContentType(),
                    analysis.get().getDescription());
        } catch (RuntimeException e) {
            logger.warn("Analyzing photo {} failed: {}", id, e.getMessage());
            return Optional.ofNullable(transactionTemplate.execute((status) -> failed(id, e)));
        }

        List<Item> items = detected.stream().map(this::match).toList();
        transactionTemplate.executeWithoutResult((status) -> ready(id, items));
        return Optional.empty();
    }

    Item match(DetectedFood detected) {
        String unit = normalizeUnit(detected.unit());
        double amount = detected.amount() == null || detected.amount() <= 0 ? 1 : detected.amount();
        double gramsPerUnit = gramsPerUnit(unit, detected.gramsPerUnit());
        String name = detected.name().strip();

        Optional<FoodExtractor.Match> match = foodExtractor.findBestMatch(name);
        Long foodId = match.map(FoodExtractor.Match::food).map(Food::getId).orElse(null);
        Long openFoodId = match.map(FoodExtractor.Match::openFood).map((openFood) -> openFood.getId()).orElse(null);
        return new Item(name, amount, unit, gramsPerUnit, foodId, openFoodId);
    }

    private void ready(Long id, List<Item> items) {
        FoodImageAnalysis analysis = analysisRepository.findById(id).orElse(null);
        if (analysis == null || analysis.getStatus() != FoodImageStatus.PENDING)
            return;
        analysis.setItems(items);
        analysis.setStatus(FoodImageStatus.READY);
        analysis.setAnalyzedAt(Instant.now());
        analysis.setError(null);
        analysisRepository.save(analysis);

        String message = items.isEmpty()
                ? "Auf dem Foto wurde kein Essen erkannt."
                : items.size() + " Lebensmittel erkannt: "
                        + String.join(", ", items.stream().map(Item::name).toList())
                        + ". Prüfe die Mengen und trag sie ein.";
        notificationService.create(analysis.getUser().getId(), new CreateNotificationDto(
                items.isEmpty() ? NotificationType.WARNING : NotificationType.SUCCESS,
                "Foto ausgewertet", message, link(id), "Prüfen"));
    }

    // the time of the next attempt, null after the last one
    private Instant failed(Long id, RuntimeException e) {
        FoodImageAnalysis analysis = analysisRepository.findById(id).orElse(null);
        if (analysis == null || analysis.getStatus() != FoodImageStatus.PENDING)
            return null;
        analysis.setAttempts(analysis.getAttempts() + 1);
        String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        analysis.setError(error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error);

        if (analysis.getAttempts() < MAX_ATTEMPTS) {
            // e.g. the per minute quota of the free tier, waiting a bit usually helps
            analysis.setNextAttemptAt(Instant.now().plus(Duration.ofMinutes(analysis.getAttempts())));
            analysisRepository.save(analysis);
            return analysis.getNextAttemptAt();
        }
        analysis.setStatus(FoodImageStatus.FAILED);
        analysisRepository.save(analysis);
        logger.error("Giving up on photo {} after {} attempts: {}", id, analysis.getAttempts(), error);
        notificationService.create(analysis.getUser().getId(), new CreateNotificationDto(NotificationType.ERROR,
                "Foto konnte nicht ausgewertet werden",
                "Versuch es später noch einmal, das Foto zählt nicht zu deinem Tageslimit.", link(id), "Ansehen"));
        return null;
    }

    // ---- helpers ----

    private long countToday(String userId) {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        return analysisRepository.countByUserIdAndCreatedAtGreaterThanEqualAndStatusNotIn(userId, startOfDay,
                Set.of(FoodImageStatus.FAILED));
    }

    private FoodImageAnalysis findAnalysis(String userId, Long id) {
        return analysisRepository.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.foodImage(id));
    }

    private FoodPortion findOrCreatePortion(Food food, String unit, double gramsPerUnit) {
        Optional<FoodPortion> existing = food.getPortions().stream()
                .filter((portion) -> portion.getLabel() != null && portion.getLabel().strip().equalsIgnoreCase(unit))
                .findFirst();
        if (existing.isPresent())
            return existing.get();

        food.addPortion(new FoodPortion(null, null, new ArrayList<>(), unit, gramsPerUnit, false));
        // the portion needs its id before a tracked food can reference it
        Food saved = foodRepository.saveAndFlush(food);
        return saved.getPortions().stream()
                .filter((portion) -> unit.equalsIgnoreCase(portion.getLabel()))
                .findFirst().orElseThrow();
    }

    private FoodImageAnalysisDto toDto(FoodImageAnalysis analysis) {
        List<FoodImageItemDto> items = analysis.getItems().stream().map(this::toItemDto).toList();
        return new FoodImageAnalysisDto(analysis.getId(), analysis.getStatus(), analysis.getMeal(),
                analysis.getDescription(), items,
                analysis.getStatus() == FoodImageStatus.FAILED ? analysis.getError() : null,
                analysis.getCreatedAt(), analysis.getAnalyzedAt(), analysis.getReviewedAt());
    }

    private FoodImageItemDto toItemDto(Item item) {
        if (item.foodId() != null) {
            return new FoodImageItemDto(item.name(), item.amount(), item.unit(), item.gramsPerUnit(),
                    foodRepository.findById(item.foodId()).map(foodMapper::mapFromEntityToFoodWithPortionsDto)
                            .orElse(null),
                    null);
        }
        if (item.openFoodId() != null) {
            return openFoodRepository.findById(item.openFoodId())
                    .map((openFood) -> new FoodImageItemDto(item.name(), item.amount(), item.unit(),
                            item.gramsPerUnit(), FoodMapper.mapFromOpenFoodToFoodWithPortionsDto(openFood),
                            openFood.getId()))
                    .orElse(new FoodImageItemDto(item.name(), item.amount(), item.unit(), item.gramsPerUnit(), null,
                            null));
        }
        return new FoodImageItemDto(item.name(), item.amount(), item.unit(), item.gramsPerUnit(), null, null);
    }

    static String link(Long id) {
        return "/foods/images/" + id;
    }

    static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank())
            return "g";
        String trimmed = unit.strip();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.equals("g") || lower.equals("gramm"))
            return "g";
        if (lower.equals("ml") || lower.equals("milliliter"))
            return "ml";
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    static double gramsPerUnit(String unit, Double estimated) {
        if (GRAM_UNITS.contains(unit))
            return 1;
        return estimated == null || estimated <= 0 ? DEFAULT_GRAMS_PER_UNIT : estimated;
    }

    private static MealType parseMeal(String meal) {
        try {
            return MealType.valueOf(meal == null ? "" : meal.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BadRequestException.meal(meal);
        }
    }
}
