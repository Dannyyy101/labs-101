import SwiftUI

/// Status of the Apple Health sync and what the backend has stored.
struct HealthSyncView: View {
    @State private var status = HealthSyncStatus.shared
    @State private var needsAuthorization = false
    @State private var authorizationError: String?
    @State private var summary: [HealthTypeSummary] = []
    @State private var summaryError: String?
    @State private var confirmResync = false

    private let service = HealthSyncService()

    var body: some View {
        NavigationStack {
            List {
                if needsAuthorization {
                    authorizationSection
                }
                syncSection
                storedSection
            }
            .navigationTitle("Health")
            .refreshable { await sync() }
            .task {
                needsAuthorization = await HealthAuthorization.needsRequest()
                await loadSummary()
            }
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

            Button("Jetzt synchronisieren", systemImage: "arrow.triangle.2.circlepath") {
                Task { await sync() }
            }
            .disabled(status.isRunning)

            Button("Alles neu synchronisieren", systemImage: "arrow.counterclockwise", role: .destructive) {
                confirmResync = true
            }
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

    private func sync() async {
        await HealthSyncEngine.shared.sync()
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
