package com.labs_101.backend.foodImage;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.labs_101.backend.repositories.FoodImageAnalysisRepository;
import com.labs_101.backend.services.FoodImageService;

/**
 * Analyzes a photo in the background right after its upload is committed, the
 * request doesn't wait for Gemini. Failed attempts are scheduled again at the
 * time the service decides, photos still pending after a restart (lost from
 * memory) are picked up on start. The small pool keeps us below the requests
 * per minute of the free tier.
 */
@Component
public class FoodImageJob implements DisposableBean {
    static final int THREADS = 2;

    private final Logger logger = LoggerFactory.getLogger(FoodImageJob.class);

    private final FoodImageService foodImageService;
    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

    FoodImageJob(FoodImageService foodImageService) {
        this.foodImageService = foodImageService;
        scheduler.setPoolSize(THREADS);
        scheduler.setThreadNamePrefix("food-image-");
        scheduler.initialize();
    }

    // after the commit, otherwise the job could run before the photo is in the database
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploaded(FoodImageService.UploadedEvent event) {
        scheduler.execute(() -> run(event.id()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumePending() {
        for (FoodImageAnalysisRepository.Pending pending : foodImageService.findPending())
            schedule(pending.id(), pending.nextAttemptAt());
    }

    void run(Long id) {
        try {
            Optional<Instant> retryAt = foodImageService.analyze(id);
            retryAt.ifPresent((at) -> schedule(id, at));
        } catch (RuntimeException e) {
            // e.g. the database is gone, the photo stays pending and is tried again on the next start
            logger.error("Job failed for photo {}", id, e);
        }
    }

    private void schedule(Long id, Instant at) {
        scheduler.schedule(() -> run(id), at.isBefore(Instant.now()) ? Instant.now() : at);
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }
}
