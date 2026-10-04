package com.labs_101.backend.services;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.labs_101.backend.dtos.run.BestEffortDto;
import com.labs_101.backend.dtos.run.RunDetailDto;
import com.labs_101.backend.dtos.run.RunPointDto;
import com.labs_101.backend.dtos.run.RunSplitDto;
import com.labs_101.backend.dtos.run.RunSummaryDto;
import com.labs_101.backend.exception.NotFoundException;
import com.labs_101.backend.repositories.RunRepository;
import com.labs_101.backend.repositories.RunRepository.HeartRateRow;
import com.labs_101.backend.repositories.RunRepository.RunRow;
import com.labs_101.backend.repositories.RunRepository.ZoneRow;

import tools.jackson.databind.JsonNode;

/**
 * Running workouts for the run dashboard, computed from the Apple Health
 * workouts the app syncs: statistics and metadata of the workout, its GPS
 * route and the heart rate samples recorded during it.
 */
@Service
public class RunService {

    /** Lower bounds of Z2…Z5 in bpm, below the first one is Z1. */
    public static final double[] ZONE_BOUNDS = { 135, 148, 160, 171 };

    private static final double[] BEST_EFFORTS = { 5000, 10000, 21097.5, 42195 };
    private static final String[] BEST_EFFORT_NAMES = { "5 km", "10 km", "Halbmarathon", "Marathon" };

    /** Route points per run when listing, enough for the thumbnail, elevation and best efforts. */
    private static final int LIST_ROUTE_POINTS = 600;
    private static final int THUMBNAIL_POINTS = 60;
    /** Distance between two points of the track of a single run. */
    private static final double POINT_DISTANCE = 25;
    /** Points before and after a point the pace is averaged over (±100 m). */
    private static final int PACE_WINDOW = 4;
    /** GPS fixes less accurate than this are ignored. */
    private static final double MAX_ACCURACY = 50;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");

    private static final String DISTANCE = "HKQuantityTypeIdentifierDistanceWalkingRunning";
    private static final String HEART_RATE = "HKQuantityTypeIdentifierHeartRate";
    private static final String ENERGY = "HKQuantityTypeIdentifierActiveEnergyBurned";
    private static final String STEPS = "HKQuantityTypeIdentifierStepCount";

    private final RunRepository runRepository;

    RunService(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public List<RunSummaryDto> getRuns(String userId, Instant from, Instant to) {
        if (from != null && to != null && to.isBefore(from))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to is before from");
        Map<UUID, ZoneRow> zones = runRepository.heartRateZones(userId, from, to, null, ZONE_BOUNDS);
        return runRepository.findRuns(userId, from, to, LIST_ROUTE_POINTS).stream()
                .map(row -> summary(row, Track.of(row), zones.get(row.uuid())))
                .toList();
    }

    public RunDetailDto getRun(String userId, UUID id) {
        RunRow row = runRepository.findRun(userId, id, Integer.MAX_VALUE).stream().findFirst()
                .orElseThrow(() -> NotFoundException.run(id));
        Track track = Track.of(row);
        ZoneRow zones = runRepository.heartRateZones(userId, null, null, id, ZONE_BOUNDS).get(id);
        List<HeartRateRow> heartRate = runRepository.heartRate(userId, row.startDate(), row.endDate());

        List<RunPointDto> points = resample(track, row.startDate(), heartRate);
        return new RunDetailDto(summary(row, track, zones), points, splits(points));
    }

    // MARK: summary

    private static RunSummaryDto summary(RunRow row, Track track, ZoneRow zones) {
        JsonNode statistics = row.statistics();
        double duration = row.duration() != null ? row.duration()
                : (row.endDate().toEpochMilli() - row.startDate().toEpochMilli()) / 1000.0;
        double distance = track.distance();

        Double averageHeartRate = heartRate(statistics, "average");
        Double maxHeartRate = heartRate(statistics, "maximum");
        if (zones != null && zones.seconds() > 0) {
            if (averageHeartRate == null)
                averageHeartRate = zones.weightedBpm() / zones.seconds();
            if (maxHeartRate == null)
                maxHeartRate = zones.maxBpm();
        }
        Double steps = statistic(statistics, STEPS, "sum");
        Double elevation = quantityInMeters(text(row.metadata(), "HKElevationAscended"));

        ZoneId zone = timeZone(row.metadata());
        return new RunSummaryDto(
                row.uuid(),
                name(row.startDate().atZone(zone).getHour(), distance),
                row.startDate(),
                row.endDate(),
                zone.getId(),
                isTrue(row.metadata(), "HKIndoorWorkout") || track.isEmpty(),
                row.sourceName(),
                distance,
                duration,
                elevation != null ? elevation : track.elevationGain(),
                averageHeartRate,
                maxHeartRate,
                statistic(statistics, ENERGY, "sum"),
                steps != null && duration > 0 ? steps / (duration / 60) : null,
                zones != null ? zones.zoneSeconds() : new double[ZONE_BOUNDS.length + 1],
                track.thumbnail(THUMBNAIL_POINTS),
                track.bestEfforts());
    }

    static String name(int hour, double distance) {
        if (distance >= 42195)
            return "Marathon";
        if (distance >= 21097.5)
            return "Halbmarathon";
        if (hour < 11)
            return "Morgenlauf";
        if (hour < 14)
            return "Mittagslauf";
        if (hour < 18)
            return "Nachmittagslauf";
        return "Abendlauf";
    }

    // MARK: track of a single run

    /** The track every {@link #POINT_DISTANCE} meters, with pace and heart rate. */
    private static List<RunPointDto> resample(Track track, Instant start, List<HeartRateRow> heartRate) {
        if (track.isEmpty())
            return List.of();
        int n = track.size();
        double total = track.distanceAt(n - 1);
        List<double[]> samples = new ArrayList<>(); // distance, moving time, lat, lon, elevation, elapsed time
        int i = 0;
        for (double d = 0;; d = Math.min(d + POINT_DISTANCE, total)) {
            while (i < n - 2 && track.distanceAt(i + 1) < d)
                i++;
            double span = track.distanceAt(i + 1) - track.distanceAt(i);
            double f = span > 0 ? Math.clamp((d - track.distanceAt(i)) / span, 0, 1) : 0;
            samples.add(new double[] {
                    d,
                    lerp(track.movingTime(i), track.movingTime(i + 1), f),
                    lerp(track.latitude(i), track.latitude(i + 1), f),
                    lerp(track.longitude(i), track.longitude(i + 1), f),
                    lerp(track.altitude(i), track.altitude(i + 1), f),
                    lerp(track.elapsed(i), track.elapsed(i + 1), f) });
            if (d >= total)
                break;
        }

        HeartRateLookup lookup = new HeartRateLookup(heartRate, start);
        List<RunPointDto> points = new ArrayList<>(samples.size());
        for (int k = 0; k < samples.size(); k++) {
            double[] s = samples.get(k);
            double[] a = samples.get(Math.max(0, k - PACE_WINDOW));
            double[] b = samples.get(Math.min(samples.size() - 1, k + PACE_WINDOW));
            Double pace = b[0] - a[0] > 1 ? (b[1] - a[1]) / (b[0] - a[0]) * 1000 : null;
            points.add(new RunPointDto(s[0], s[1], s[2], s[3], Double.isNaN(s[4]) ? null : s[4], pace,
                    lookup.at(s[5])));
        }
        return points;
    }

    /** Full kilometers, plus the rest if it's longer than 50 m. */
    private static List<RunSplitDto> splits(List<RunPointDto> points) {
        List<RunSplitDto> splits = new ArrayList<>();
        if (points.size() < 2)
            return splits;
        double total = points.getLast().distance();
        for (int k = 0; k * 1000 < total; k++) {
            double from = k * 1000, to = Math.min(total, (k + 1) * 1000);
            if (to - from < 50)
                break;
            double sum = 0;
            int count = 0;
            for (RunPointDto p : points) {
                if (p.distance() > from && p.distance() <= to && p.heartRate() != null) {
                    sum += p.heartRate();
                    count++;
                }
            }
            splits.add(new RunSplitDto(k, to - from, timeAt(points, to) - timeAt(points, from),
                    count > 0 ? sum / count : null));
        }
        return splits;
    }

    private static double timeAt(List<RunPointDto> points, double distance) {
        for (int i = 1; i < points.size(); i++) {
            RunPointDto a = points.get(i - 1), b = points.get(i);
            if (b.distance() >= distance) {
                double span = b.distance() - a.distance();
                return lerp(a.time(), b.time(), span > 0 ? (distance - a.distance()) / span : 0);
            }
        }
        return points.getLast().time();
    }

    /** Heart rate at a time of the run, interpolated between samples that are at most a minute apart. */
    private static final class HeartRateLookup {
        private final double[] times;
        private final double[] values;
        private int index;

        HeartRateLookup(List<HeartRateRow> rows, Instant start) {
            times = new double[rows.size()];
            values = new double[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                times[i] = (rows.get(i).date().toEpochMilli() - start.toEpochMilli()) / 1000.0;
                values[i] = rows.get(i).bpm();
            }
        }

        /** {@code elapsed} has to grow from call to call. */
        Double at(double elapsed) {
            if (times.length == 0)
                return null;
            while (index < times.length - 1 && times[index + 1] <= elapsed)
                index++;
            if (times[index] > elapsed)
                return elapsed > times[index] - 60 ? values[index] : null;
            if (index == times.length - 1)
                return elapsed - times[index] < 60 ? values[index] : null;
            double gap = times[index + 1] - times[index];
            if (gap > 60)
                return elapsed - times[index] < 30 ? values[index] : null;
            return lerp(values[index], values[index + 1], (elapsed - times[index]) / gap);
        }
    }

    /**
     * GPS route of a workout: accurate fixes only, cumulative distance scaled
     * to the distance Apple Health measured, and the moving time at every
     * point (the elapsed time without pauses).
     */
    static final class Track {
        private final double[] elapsed;
        private final double[] moving;
        private final double[] lat;
        private final double[] lon;
        private final double[] alt;
        private final double[] dist;
        private final double distance;

        private Track(List<double[]> points, List<double[]> pauses, Double measuredDistance) {
            int n = points.size();
            elapsed = new double[n];
            moving = new double[n];
            lat = new double[n];
            lon = new double[n];
            alt = new double[n];
            dist = new double[n];
            for (int i = 0; i < n; i++) {
                double[] p = points.get(i);
                elapsed[i] = p[0];
                lat[i] = p[1];
                lon[i] = p[2];
                alt[i] = p[3];
                // the first fix may be from shortly before the workout started
                moving[i] = Math.max(0, p[0] - pausedBefore(pauses, p[0]));
                if (i > 0)
                    dist[i] = dist[i - 1] + haversine(lat[i - 1], lon[i - 1], lat[i], lon[i]);
            }
            double measured = n > 0 ? dist[n - 1] : 0;
            if (measuredDistance != null && measuredDistance > 0 && measured > 0) {
                double scale = measuredDistance / measured;
                for (int i = 0; i < n; i++)
                    dist[i] *= scale;
            }
            this.distance = measuredDistance != null && measuredDistance > 0 ? measuredDistance : measured;
        }

        static Track of(RunRow row) {
            List<double[]> points = new ArrayList<>();
            if (row.track() != null) {
                for (JsonNode p : row.track()) {
                    double accuracy = p.path(6).asDouble(0);
                    if (p.size() < 3 || accuracy < 0 || accuracy > MAX_ACCURACY)
                        continue;
                    points.add(new double[] { p.get(0).asDouble(), p.get(1).asDouble(), p.get(2).asDouble(),
                            p.path(3).isNumber() ? p.get(3).asDouble() : Double.NaN });
                }
            }
            if (points.size() < 2)
                points = List.of();
            return new Track(points, pauses(row), statistic(row.statistics(), DISTANCE, "sum"));
        }

        boolean isEmpty() {
            return lat.length == 0;
        }

        int size() {
            return lat.length;
        }

        double distance() {
            return distance;
        }

        double distanceAt(int i) {
            return dist[i];
        }

        double movingTime(int i) {
            return moving[i];
        }

        double elapsed(int i) {
            return elapsed[i];
        }

        double latitude(int i) {
            return lat[i];
        }

        double longitude(int i) {
            return lon[i];
        }

        double altitude(int i) {
            return alt[i];
        }

        /** Climbs of less than 3 m are GPS noise. */
        Double elevationGain() {
            double gain = 0, low = Double.NaN;
            for (double a : alt) {
                if (Double.isNaN(a))
                    continue;
                if (Double.isNaN(low) || a < low) {
                    low = a;
                } else if (a - low >= 3) {
                    gain += a - low;
                    low = a;
                }
            }
            return Double.isNaN(low) ? null : gain;
        }

        List<double[]> thumbnail(int maxPoints) {
            List<double[]> points = new ArrayList<>();
            int step = Math.max(1, size() / maxPoints);
            for (int i = 0; i < size(); i += step)
                points.add(new double[] { round(lat[i], 5), round(lon[i], 5) });
            if (!isEmpty() && (size() - 1) % step != 0)
                points.add(new double[] { round(lat[size() - 1], 5), round(lon[size() - 1], 5) });
            return points;
        }

        /** Fastest stretch of every standard distance the track is long enough for. */
        List<BestEffortDto> bestEfforts() {
            List<BestEffortDto> efforts = new ArrayList<>();
            for (int e = 0; e < BEST_EFFORTS.length; e++) {
                double target = BEST_EFFORTS[e];
                if (isEmpty() || dist[size() - 1] < target)
                    break;
                double best = Double.MAX_VALUE;
                int i = 0;
                for (int j = 1; j < size(); j++) {
                    // the latest start that still leaves `target` meters until j
                    while (i + 1 < j && dist[j] - dist[i + 1] >= target)
                        i++;
                    double covered = dist[j] - dist[i];
                    if (covered >= target)
                        best = Math.min(best, (moving[j] - moving[i]) * target / covered);
                }
                if (best < Double.MAX_VALUE)
                    efforts.add(new BestEffortDto(BEST_EFFORT_NAMES[e], target, best));
            }
            return efforts;
        }
    }

    /** Pause intervals in seconds since the start (pause/resume and motion paused/resumed events). */
    private static List<double[]> pauses(RunRow row) {
        List<double[]> pauses = new ArrayList<>();
        if (row.events() == null)
            return pauses;
        double pausedAt = Double.NaN;
        for (JsonNode event : row.events()) {
            int type = event.path("type").asInt(0);
            double at = secondsSince(row.startDate(), event.path("startDate").asString(null));
            if (Double.isNaN(at))
                continue;
            // HKWorkoutEventType: 1 pause, 2 resume, 3 motion paused, 4 motion resumed
            if ((type == 1 || type == 3) && Double.isNaN(pausedAt)) {
                pausedAt = at;
            } else if ((type == 2 || type == 4) && !Double.isNaN(pausedAt)) {
                pauses.add(new double[] { pausedAt, at });
                pausedAt = Double.NaN;
            }
        }
        return pauses;
    }

    private static double pausedBefore(List<double[]> pauses, double t) {
        double paused = 0;
        for (double[] p : pauses)
            paused += Math.max(0, Math.min(t, p[1]) - p[0]);
        return paused;
    }

    // MARK: helpers

    private static Double statistic(JsonNode statistics, String type, String field) {
        if (statistics == null)
            return null;
        JsonNode value = statistics.path(type).path(field);
        if (!value.isNumber())
            return null;
        String unit = statistics.path(type).path("unit").asString("");
        double v = value.asDouble();
        return switch (unit) {
            case "km" -> v * 1000;
            case "count/s" -> v * 60;
            case "kJ" -> v / 4.184;
            default -> v;
        };
    }

    private static Double heartRate(JsonNode statistics, String field) {
        return statistic(statistics, HEART_RATE, field);
    }

    private static final Pattern QUANTITY = Pattern.compile("^\\s*(-?[\\d.]+)\\s*(\\w+)");

    /** Quantity metadata is stored as its description, e.g. "1234 cm" or "12.3 m". */
    static Double quantityInMeters(String quantity) {
        if (quantity == null)
            return null;
        Matcher m = QUANTITY.matcher(quantity);
        if (!m.find())
            return null;
        try {
            double v = Double.parseDouble(m.group(1));
            return switch (m.group(2)) {
                case "m" -> v;
                case "cm" -> v / 100;
                case "km" -> v * 1000;
                case "ft" -> v * 0.3048;
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        return node == null || !node.path(field).isString() ? null : node.path(field).asString();
    }

    private static boolean isTrue(JsonNode node, String field) {
        if (node == null)
            return false;
        JsonNode value = node.path(field);
        return value.isBoolean() ? value.asBoolean() : value.isNumber() && value.asDouble() != 0;
    }

    private static ZoneId timeZone(JsonNode metadata) {
        String id = text(metadata, "HKTimeZone");
        if (id == null)
            return DEFAULT_ZONE;
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            return DEFAULT_ZONE;
        }
    }

    private static double secondsSince(Instant start, String iso) {
        if (iso == null)
            return Double.NaN;
        try {
            return (Instant.parse(iso).toEpochMilli() - start.toEpochMilli()) / 1000.0;
        } catch (DateTimeException e) {
            return Double.NaN;
        }
    }

    static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double r = Math.PI / 180;
        double dLat = (lat2 - lat1) * r, dLon = (lon2 - lon1) * r;
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(lat1 * r) * Math.cos(lat2 * r) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6371000 * Math.asin(Math.sqrt(h));
    }

    private static double lerp(double a, double b, double f) {
        return a + (b - a) * f;
    }

    private static double round(double v, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(v * factor) / factor;
    }
}
