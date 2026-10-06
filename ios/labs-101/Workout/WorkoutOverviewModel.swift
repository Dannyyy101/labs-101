import Foundation
import Observation

/// Templates, the running workout and the history, the data of /workouts in the web.
@Observable
final class WorkoutOverviewModel {
    private(set) var templates: [WorkoutTemplate] = []
    /// finished workouts of the last year, newest first, also for the "last time" hints
    private(set) var history: [WorkoutSession] = []
    private(set) var active: WorkoutSession?
    private(set) var exercises: [Exercise] = []
    private(set) var isLoading = false
    private(set) var hasLoaded = false
    /// set when loading failed, changes are reported through `actionError`
    private(set) var loadError: String?
    var actionError: String?

    private let calendar: Calendar

    init(calendar: Calendar = .current) {
        self.calendar = calendar
    }

    func load(using service: WorkoutService) async {
        isLoading = true
        defer { isLoading = false }

        let now = Date.now
        let from = calendar.date(byAdding: .year, value: -1, to: now) ?? now
        do {
            async let templates = service.templates()
            async let history = service.sessions(from: from, to: now.addingTimeInterval(86_400))
            async let active = service.activeSession()
            async let exercises = service.exercises()
            (self.templates, self.history, self.active, self.exercises) = try await (templates, history, active, exercises)
            loadError = nil
            hasLoaded = true
        } catch is CancellationError {
        } catch {
            loadError = error.localizedDescription
        }
    }

    /// Starts the workout, or returns the running one when there already is one.
    func start(templateID: Int?, using service: WorkoutService) async -> WorkoutSession? {
        do {
            let session = try await service.start(workoutID: templateID)
            active = session
            return session
        } catch APIError.server(status: 400, _) {
            // another workout is still running, open that one instead
            active = try? await service.activeSession()
            return active
        } catch {
            actionError = error.localizedDescription
            return nil
        }
    }

    func delete(_ session: WorkoutSession, using service: WorkoutService) async {
        // optimistic, so the row disappears right away
        let previous = history
        history.removeAll { $0.id == session.id }
        do {
            try await service.delete(session.id)
        } catch {
            history = previous
            actionError = error.localizedDescription
        }
    }

    func exercise(_ id: Int?) -> Exercise? {
        guard let id else { return nil }
        return exercises.first { $0.id == id }
    }

    func lastSession(of template: WorkoutTemplate) -> WorkoutSession? {
        history.first { $0.workoutId == template.id }
    }

    // MARK: Last 30 days

    var recent: [WorkoutSession] {
        let from = calendar.date(byAdding: .day, value: -30, to: .now) ?? .now
        return history.filter { $0.startedAt >= from }
    }
}
