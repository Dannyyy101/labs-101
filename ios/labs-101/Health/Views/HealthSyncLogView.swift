import SwiftUI

/// The last background runs of the sync: when iOS woke the app and how far it got.
struct HealthSyncLogView: View {
    @State private var entries = HealthSyncLog.entries

    var body: some View {
        List {
            if entries.isEmpty {
                Text("Noch keine Hintergrund-Syncs. iOS entscheidet selbst, wann die App geweckt wird, meist wenn neue Health-Daten da sind.")
                    .foregroundStyle(.secondary)
            }
            ForEach(entries) { entry in
                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        Text(entry.trigger.label).font(.headline)
                        Spacer()
                        Text(entry.date, format: .dateTime.day().month().hour().minute())
                            .foregroundStyle(.secondary)
                    }
                    Text(result(entry))
                        .foregroundStyle(entry.error != nil && !entry.locked ? .red : .primary)
                    if !entry.types.isEmpty {
                        Text(entry.types.joined(separator: ", "))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                }
                .font(.subheadline)
            }
        }
        .navigationTitle("Hintergrund-Syncs")
        .toolbar {
            if !entries.isEmpty {
                Button("Leeren", role: .destructive) {
                    HealthSyncLog.clear()
                    entries = []
                }
            }
        }
        .refreshable { entries = HealthSyncLog.entries }
    }

    private func result(_ entry: HealthSyncLogEntry) -> String {
        let duration = "\(Int(entry.seconds.rounded())) s"
        if entry.locked { return "iPhone gesperrt, Health-Daten nicht lesbar · \(duration)" }
        if let error = entry.error { return "\(error) · \(duration)" }
        if entry.joined { return "Lief schon, nur angehängt" }
        let uploaded = "\(entry.uploaded) Einträge hochgeladen"
        return entry.stoppedEarly ? "\(uploaded), Zeit abgelaufen · \(duration)" : "\(uploaded), fertig · \(duration)"
    }
}
