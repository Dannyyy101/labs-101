import SwiftUI

/// Status of the Apple Health sync and what the backend has stored.
struct HealthSyncView: View {
    @State private var status = HealthSyncStatus.shared
    @State private var needsAuthorization = false
    @State private var authorizationError: String?
    @State private var summary: [HealthTypeSummary] = []
    @State private var summaryError: String?
    @State private var confirmResync = false
    @State private var profile: UserProfile?
    @AppStorage(AppNotifications.sleepKey) private var notifySleep = true
    @AppStorage(AppNotifications.workoutKey) private var notifyWorkout = true
    @AppStorage(AppNotifications.caloriesKey) private var notifyCalories = true
    @AppStorage(AppNotifications.inboxKey) private var notifyInbox = true
    @State private var testLog: [String] = []

    private let service = HealthSyncService()

    var body: some View {
        NavigationStack {
            List {
                if needsAuthorization {
                    authorizationSection
                }
                syncSection
                notificationSection
                storedSection
                accountSection
            }
            .navigationTitle("Health")
            .refreshable {
                await sync()
                await loadProfile()
            }
            .task {
                needsAuthorization = await HealthAuthorization.needsRequest()
                await loadSummary()
            }
            .task { await loadProfile() }
            .onChange(of: status.isRunning) { _, isRunning in
                if !isRunning {
                    Task { await loadSummary() }
                }
            }
            .confirmationDialog("Alle Daten neu synchronisieren?", isPresented: $confirmResync, titleVisibility: .visible) {
                Button("Neu synchronisieren", role: .destructive) {
                    Task { await HealthSyncEngine.shared.resetAndSync() }
                }
            } message: {
                Text("Alle Health-Daten werden erneut hochgeladen. Es entstehen keine Duplikate, in Health gelöschte Einträge werden auch im Backend entfernt.")
            }
        }
    }

    private var authorizationSection: some View {
        Section {
            Button("Zugriff auf Health erlauben", systemImage: "heart.text.square") {
                Task {
                    do {
                        try await HealthAuthorization.request()
                        needsAuthorization = await HealthAuthorization.needsRequest()
                        await sync()
                    } catch {
                        authorizationError = error.localizedDescription
                    }
                }
            }
            if let authorizationError {
                Text(authorizationError).foregroundStyle(.red)
            }
        } footer: {
            Text("Erlaube am besten alle Kategorien, nur freigegebene Daten werden synchronisiert.")
        }
    }

    private var syncSection: some View {
        Section("Synchronisierung") {
            if status.isRunning {
                HStack {
                    ProgressView()
                    VStack(alignment: .leading) {
                        Text(status.isFullSync ? "Kompletter Sync: \(status.currentType ?? "…")" : "Sync: \(status.currentType ?? "…")")
                        Text("\(status.uploadedInRun) Einträge hochgeladen")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            LabeledContent("Letzter vollständiger Sync") {
                if let lastSync = status.lastSync {
                    Text(lastSync, format: .relative(presentation: .named))
                } else {
                    Text("noch nie")
                }
            }
            if status.pendingFullSyncs > 0 {
                LabeledContent("Noch nicht komplett synchronisiert", value: "\(status.pendingFullSyncs) Typen")
            }
            if let report = status.lastReport {
                if let error = report.error {
                    Text(error).foregroundStyle(.red)
                } else if report.stoppedEarly {
                    Text("Wird beim nächsten Sync fortgesetzt").foregroundStyle(.secondary)
                }
            }

            NavigationLink {
                HealthSyncLogView()
            } label: {
                Label("Hintergrund-Syncs", systemImage: "moon.zzz")
            }

            Button("Jetzt synchronisieren", systemImage: "arrow.triangle.2.circlepath") {
                Task { await sync() }
            }
            .disabled(status.isRunning)

            Button("Alles neu synchronisieren", systemImage: "arrow.counterclockwise", role: .destructive) {
                confirmResync = true
            }
        }
    }

    private var notificationSection: some View {
        Section {
            Toggle("Schlaf am Morgen", systemImage: "bed.double.fill", isOn: $notifySleep)
            Toggle("Nach dem Training", systemImage: "figure.run", isOn: $notifyWorkout)
            Toggle("Offene Kalorien um \(AppNotifications.eveningHour) Uhr", systemImage: "fork.knife", isOn: $notifyCalories)
                .onChange(of: notifyCalories) { _, isOn in
                    if !isOn { AppNotifications.cancelEveningReminder() }
                }
            Toggle("Benachrichtigungen aus dem Web", systemImage: "bell.fill", isOn: $notifyInbox)
            #if DEBUG
            Button("Test-Benachrichtigungen senden", systemImage: "paperplane") {
                Task { testLog = await AppNotifications.sendTestNotifications() }
            }
            ForEach(testLog, id: \.self) { line in
                Text(line).font(.caption).foregroundStyle(.secondary)
            }
            #endif
        } header: {
            Text("Benachrichtigungen")
        } footer: {
            Text("Werden nach dem Health-Sync geprüft, auch im Hintergrund. Am Abend nur, wenn noch mindestens \(AppNotifications.openKcalThreshold.roundedFormatted) kcal offen sind.")
        }
    }

    private var storedSection: some View {
        Section {
            if let summaryError {
                Text(summaryError).foregroundStyle(.red)
            }
            ForEach(summary) { type in
                VStack(alignment: .leading) {
                    HStack {
                        Text(HealthSyncStatus.displayName(of: type.type))
                        Spacer()
                        Text(type.count, format: .number)
                            .monospacedDigit()
                            .foregroundStyle(.secondary)
                    }
                    if let lastDate = type.lastDate {
                        Text("zuletzt \(lastDate, format: .dateTime.day().month().year().hour().minute())")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        } header: {
            Text("Im Backend gespeichert")
        } footer: {
            if !summary.isEmpty {
                Text("\(summary.reduce(0) { $0 + $1.count }) Einträge in \(summary.count) Typen")
            }
        }
    }

    private var accountSection: some View {
        Section {
            HStack(spacing: 12) {
                ProfileImage(profile: profile)
                VStack(alignment: .leading, spacing: 2) {
                    Text(profile?.name ?? " ")
                        .font(.headline)
                    Text(profile?.email ?? " ")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }
            if let url = ProfileService.editURL() {
                Link(destination: url) {
                    Label("Profil & Profilbild bearbeiten", systemImage: "person.crop.circle")
                }
            }
            Button("Abmelden", systemImage: "rectangle.portrait.and.arrow.right", role: .destructive) {
                Task { await AuthSession.shared.signOut() }
            }
        } header: {
            Text("Konto")
        } footer: {
            Text("Name und Profilbild werden bei Zitadel geändert. Danach hier zum Aktualisieren nach unten ziehen.")
        }
    }

    private func sync() async {
        await HealthSyncEngine.shared.sync()
    }

    private func loadProfile() async {
        if let loaded = try? await ProfileService().profile() {
            profile = loaded
        }
    }

    private func loadSummary() async {
        do {
            summary = try await service.summary()
            summaryError = nil
        } catch {
            summaryError = error.localizedDescription
        }
    }
}

#Preview {
    HealthSyncView()
}
