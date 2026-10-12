import Foundation
import OSLog
import UserNotifications

/// Local notifications, there is no push server. They are checked after every
/// health sync, which also runs in the background when HealthKit has new samples:
///
/// - in the morning once the sleep of the night arrived
/// - after a workout, with its numbers
/// - at 20:00 when too many calories of the goal are still open
/// - the unread notifications of the backend (the bell in the web)
enum AppNotifications {
    nonisolated static let sleepKey = "notifications.sleep"
    nonisolated static let workoutKey = "notifications.workout"
    nonisolated static let caloriesKey = "notifications.calories"
    nonisolated static let inboxKey = "notifications.inbox"

    /// the evening reminder only comes when at least this much is open
    static let openKcalThreshold: Double = 500
    static let eveningHour = 20

    /// prefix of the mirrored backend notifications, followed by their id
    nonisolated static let inboxPrefix = "inbox-"

    private static let logger = Logger(subsystem: "labs-101", category: "Notifications")
    private static let center = UNUserNotificationCenter.current()
    private static let defaults = UserDefaults.standard

    private static let eveningID = "calories-evening"
    private static let notifiedSleepDayKey = "notifications.notifiedSleepDay"
    private static let notifiedWorkoutsKey = "notifications.notifiedWorkouts"
    private static let appSessionsKey = "notifications.appSessions"
    private static let lastInboxIDKey = "notifications.lastInboxId"
    /// a workout synced later than this after its end isn't news anymore
    private static let workoutWindow: TimeInterval = 6 * 60 * 60
    /// older unread backend notifications aren't shown on the phone anymore
    private static let inboxWindow: TimeInterval = 24 * 60 * 60

    private struct Content {
        let title: String
        let body: String
    }

    /// All on by default, the toggles are in the health view.
    static func isEnabled(_ key: String) -> Bool {
        defaults.object(forKey: key) as? Bool ?? true
    }

    static func requestAuthorization() async {
        _ = try? await center.requestAuthorization(options: [.alert, .sound, .badge])
    }

    // MARK: After a sync

    static func afterSync(calendar: Calendar = .current) async {
        guard await isAuthorized() else { return }
        let now = Date.now

        if needsSleepCheck(now: now, calendar: calendar) || isEnabled(workoutKey) {
            do {
                let overview = try await OverviewService().overview(on: now, calendar: calendar)
                if needsSleepCheck(now: now, calendar: calendar) {
                    await notifySleep(overview, now: now, calendar: calendar)
                }
                if isEnabled(workoutKey) {
                    await notifyWorkouts(overview, now: now)
                }
            } catch {
                logger.warning("Loading the overview failed: \(error.localizedDescription)")
            }
        }

        if isEnabled(caloriesKey), let entries = try? await FoodService().trackedFood(on: now, calendar: calendar) {
            await updateEveningReminder(entries: entries, goals: .default, calendar: calendar)
        }

        if isEnabled(inboxKey) {
            await mirrorInbox(now: now)
        }
    }

    // MARK: Sleep

    /// Once a day, in the morning only, so a nap in the afternoon doesn't trigger it.
    private static func needsSleepCheck(now: Date, calendar: Calendar) -> Bool {
        let hour = calendar.component(.hour, from: now)
        return isEnabled(sleepKey) && (4..<13).contains(hour) && defaults.string(forKey: notifiedSleepDayKey) != now.isoDay(calendar: calendar)
    }

    private static func notifySleep(_ overview: HealthOverview, now: Date, calendar: Calendar) async {
        // the night must be over, not just the first samples of it synced
        guard let sleep = overview.sleep, sleep.end <= now, now.timeIntervalSince(sleep.end) < 12 * 60 * 60,
              let content = sleepContent(overview) else { return }
        await send(id: "sleep-\(overview.date)", content)
        defaults.set(now.isoDay(calendar: calendar), forKey: notifiedSleepDayKey)
    }

    private static func sleepContent(_ overview: HealthOverview) -> Content? {
        guard let sleep = overview.sleep else { return nil }
        var lines = ["\(sleep.asleep.sleepDuration) geschlafen, Bedarf \(sleep.need.sleepDuration)"]
        var stages: [String] = []
        if sleep.deep > 0 { stages.append("Tief \(sleep.deep.sleepDuration)") }
        if sleep.rem > 0 { stages.append("REM \(sleep.rem.sleepDuration)") }
        if sleep.awake > 0 { stages.append("Wach \(sleep.awake.sleepDuration)") }
        if !stages.isEmpty { lines.append(stages.joined(separator: " · ")) }
        if let recovery = overview.recovery.score {
            lines.append("Erholung \(recovery) % – \(OverviewStatus.recovery(recovery))")
        }
        return Content(title: "\(OverviewStatus.sleep(sleep)) · Score \(sleep.score)", body: lines.joined(separator: "\n"))
    }

    // MARK: Workouts

    private static func notifyWorkouts(_ overview: HealthOverview, now: Date) async {
        var notified = Set(defaults.stringArray(forKey: notifiedWorkoutsKey) ?? [])
        let appSessions = storedAppSessions()

        for workout in overview.workouts where now.timeIntervalSince(workout.endDate) < workoutWindow {
            let id = workout.id.uuidString
            guard !notified.contains(id) else { continue }
            notified.insert(id)
            // the watch records the workouts of the app too, those were notified when finishing them
            guard !appSessions.contains(where: { $0.overlaps(workout.startDate...workout.endDate) }) else { continue }
            await send(id: "workout-\(id)", workoutContent(workout, overview: overview))
        }
        defaults.set(Array(notified.suffix(50)), forKey: notifiedWorkoutsKey)
    }

    private static func workoutContent(_ workout: OverviewWorkout, overview: HealthOverview) -> Content {
        var parts = [workout.duration.workoutDuration]
        if let distance = workout.distance, distance > 0 {
            parts.append("\(distance.kilometers(decimals: 2)) km")
            if workout.activityType == 37 || workout.activityType == 52 {
                parts.append("\(pace(seconds: workout.duration, meters: distance)) /km")
            }
        }
        if let energy = workout.energy, energy > 0 { parts.append("\(energy.roundedFormatted) kcal") }
        if let heartRate = workout.averageHeartRate { parts.append("Ø \(heartRate.roundedFormatted) bpm") }

        let body = parts.joined(separator: " · ") + "\nBelastung heute \(overview.strain.score) – \(OverviewStatus.strain(overview.strain.score))"
        return Content(title: "\(WorkoutActivity.name(workout.activityType)) beendet", body: body)
    }

    /// Right after finishing a workout of the app, with the comparison to the last time.
    static func workoutFinished(_ session: WorkoutSession, name: String, exercises: [SessionExercise],
                                service: WorkoutService = WorkoutService()) async {
        let end = Date.now
        storeAppSession(session.startedAt...end)
        guard isEnabled(workoutKey), await isAuthorized() else { return }

        let doneSets = exercises.flatMap(\.sets).filter(\.done)
        let volume = WorkoutStats.volume(doneSets)
        let exerciseCount = exercises.filter { $0.sets.contains(where: \.done) }.count
        var lines = [
            "\(end.timeIntervalSince(session.startedAt).workoutDuration) · \(exerciseCount) Übungen · \(doneSets.count) Sätze",
            "Volumen \(WorkoutStats.formattedVolume(volume))",
        ]

        if let workoutID = session.workoutId,
           let history = try? await service.sessions(from: session.startedAt.addingTimeInterval(-90 * 86_400), to: session.startedAt),
           let previous = history.first(where: { $0.workoutId == workoutID && $0.id != session.id }) {
            let difference = volume - WorkoutStats.volume(previous.doneSets)
            let sign = difference >= 0 ? "+" : "−"
            lines[1] += " (\(sign)\(WorkoutStats.formattedVolume(abs(difference))) zum letzten Mal)"
        }

        await send(id: "session-\(session.id)", Content(title: "\(name) beendet 💪", body: lines.joined(separator: "\n")))
    }

    private static func storedAppSessions() -> [ClosedRange<Date>] {
        let pairs = defaults.array(forKey: appSessionsKey) as? [[Double]] ?? []
        return pairs.compactMap { pair in
            guard pair.count == 2, pair[0] <= pair[1] else { return nil }
            return Date(timeIntervalSince1970: pair[0])...Date(timeIntervalSince1970: pair[1])
        }
    }

    private static func storeAppSession(_ range: ClosedRange<Date>) {
        let pairs = (storedAppSessions() + [range]).suffix(10).map {
            [$0.lowerBound.timeIntervalSince1970, $0.upperBound.timeIntervalSince1970]
        }
        defaults.set(Array(pairs), forKey: appSessionsKey)
    }

    /// "5:42"
    private static func pace(seconds: Double, meters: Double) -> String {
        let perKm = Int((seconds / (meters / 1000)).rounded())
        return String(format: "%d:%02d", perKm / 60, perKm % 60)
    }

    // MARK: Calories in the evening

    /// Schedules today's reminder with the current numbers, or removes it when
    /// enough was eaten. A notification can't compute its text when it fires,
    /// so this runs after every sync and every change of today's food.
    static func updateEveningReminder(entries: [TrackedFood], goals: NutritionGoals, calendar: Calendar = .current) async {
        center.removePendingNotificationRequests(withIdentifiers: [eveningID])
        let now = Date.now
        guard isEnabled(caloriesKey), calendar.component(.hour, from: now) < eveningHour,
              let content = caloriesContent(entries: entries, goals: goals) else { return }

        var components = calendar.dateComponents([.year, .month, .day], from: now)
        components.hour = eveningHour
        await send(id: eveningID, content, trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: false))
    }

    static func cancelEveningReminder() {
        center.removePendingNotificationRequests(withIdentifiers: [eveningID])
    }

    /// nil when less than the threshold is open
    private static func caloriesContent(entries: [TrackedFood], goals: NutritionGoals) -> Content? {
        let counted = entries.filter { $0.amount > 0 }
        let kcal = counted.reduce(0) { $0 + $1.amount(of: .kcal) }
        let protein = counted.reduce(0) { $0 + $1.amount(of: .protein) }
        let open = goals.kcal - kcal
        guard open >= openKcalThreshold else { return nil }

        var body = "\(kcal.roundedFormatted) von \(goals.kcal.roundedFormatted) kcal gegessen."
        if goals.protein - protein > 0 {
            body += " Protein: noch \((goals.protein - protein).roundedFormatted) g."
        }
        return Content(title: "Noch \(open.roundedFormatted) kcal offen", body: body)
    }

    // MARK: Backend notifications

    /// Shows the new unread notifications of the backend once each, e.g. calendar
    /// invites. Without push (APNs) they arrive with the next sync.
    private static func mirrorInbox(now: Date) async {
        do {
            let unread = try await InboxService().unread()
            let lastID = defaults.integer(forKey: lastInboxIDKey)
            let new = unread.filter { $0.id > lastID && now.timeIntervalSince($0.createdAt) < inboxWindow }
            for notification in new.reversed() {
                await send(id: "\(inboxPrefix)\(notification.id)",
                           Content(title: notification.title, body: notification.message ?? ""))
            }
            if let newest = unread.map(\.id).max(), newest > lastID {
                defaults.set(newest, forKey: lastInboxIDKey)
            }
        } catch {
            logger.warning("Loading the backend notifications failed: \(error.localizedDescription)")
        }
    }

    /// Tapping a mirrored notification marks it read in the web too.
    nonisolated static func opened(identifier: String) async {
        guard identifier.hasPrefix(inboxPrefix), let id = Int(identifier.dropFirst(inboxPrefix.count)) else { return }
        try? await InboxService().markRead(id)
    }

    // MARK: Testing

    #if DEBUG
    /// Sends every kind right away with today's data, without the time windows
    /// and the "already notified" checks. The calorie reminder comes after 5 seconds,
    /// so there is time to close the app.
    static func sendTestNotifications(calendar: Calendar = .current) async -> [String] {
        await requestAuthorization()
        var log: [String] = []
        let now = Date.now

        do {
            let overview = try await OverviewService().overview(on: now, calendar: calendar)
            if let content = sleepContent(overview) {
                await send(id: "test-sleep", content)
                log.append("Schlaf gesendet")
            } else {
                log.append("Keine Schlafdaten für heute")
            }
            if let workout = overview.workouts.last {
                await send(id: "test-workout", workoutContent(workout, overview: overview))
                log.append("Training gesendet")
            } else {
                log.append("Heute kein Training in Health")
            }
        } catch {
            log.append("Übersicht: \(error.localizedDescription)")
        }

        do {
            let entries = try await FoodService().trackedFood(on: now, calendar: calendar)
            let content = caloriesContent(entries: entries, goals: .default)
                ?? Content(title: "Kalorienziel fast erreicht", body: "Weniger als \(openKcalThreshold.roundedFormatted) kcal offen, um \(eveningHour) Uhr käme keine Erinnerung.")
            await send(id: "test-calories", content, trigger: UNTimeIntervalNotificationTrigger(timeInterval: 5, repeats: false))
            log.append("Kalorien in 5 s")
        } catch {
            log.append("Essen: \(error.localizedDescription)")
        }

        do {
            try await InboxService().create(title: "Test aus dem Backend", message: "Erscheint auch in der Glocke im Web.")
            await mirrorInbox(now: .now)
            log.append("Backend-Benachrichtigung erstellt und gesendet")
        } catch {
            log.append("Backend: \(error.localizedDescription)")
        }
        return log
    }
    #endif

    // MARK: Helpers

    private static func isAuthorized() async -> Bool {
        let status = await center.notificationSettings().authorizationStatus
        return status == .authorized || status == .provisional
    }

    /// Without trigger it is shown right away.
    private static func send(id: String, _ content: Content, trigger: UNNotificationTrigger? = nil) async {
        let notification = UNMutableNotificationContent()
        notification.title = content.title
        notification.body = content.body
        notification.sound = .default
        do {
            try await center.add(UNNotificationRequest(identifier: id, content: notification, trigger: trigger))
        } catch {
            logger.error("Sending \(id) failed: \(error.localizedDescription)")
        }
    }
}
