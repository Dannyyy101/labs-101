import SwiftUI
import Vision
import VisionKit

/// Scans EAN / UPC barcodes with the camera and reports the first one found.
struct BarcodeScannerView: View {
    let onDetected: (String) -> Void

    @Environment(\.dismiss) private var dismiss

    static var isAvailable: Bool {
        DataScannerViewController.isSupported && DataScannerViewController.isAvailable
    }

    var body: some View {
        NavigationStack {
            Group {
                if Self.isAvailable {
                    DataScanner(onDetected: onDetected)
                        .ignoresSafeArea()
                } else {
                    ContentUnavailableView(
                        "Scanner nicht verfügbar",
                        systemImage: "barcode.viewfinder",
                        description: Text("Barcodes können nur auf einem Gerät mit Kamera und erteiltem Kamerazugriff gescannt werden.")
                    )
                }
            }
            .navigationTitle("Barcode scannen")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .close) { dismiss() }
                }
            }
        }
    }
}

private struct DataScanner: UIViewControllerRepresentable {
    let onDetected: (String) -> Void

    func makeUIViewController(context: Context) -> DataScannerViewController {
        let scanner = DataScannerViewController(
            recognizedDataTypes: [.barcode(symbologies: [.ean13, .ean8, .upce])],
            qualityLevel: .balanced,
            isHighlightingEnabled: true
        )
        scanner.delegate = context.coordinator
        return scanner
    }

    func updateUIViewController(_ scanner: DataScannerViewController, context: Context) {
        context.coordinator.onDetected = onDetected
        if !scanner.isScanning {
            try? scanner.startScanning()
        }
    }

    static func dismantleUIViewController(_ scanner: DataScannerViewController, coordinator: Coordinator) {
        scanner.stopScanning()
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(onDetected: onDetected)
    }

    final class Coordinator: NSObject, DataScannerViewControllerDelegate {
        var onDetected: (String) -> Void
        private var reported = false

        init(onDetected: @escaping (String) -> Void) {
            self.onDetected = onDetected
        }

        func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem], allItems: [RecognizedItem]) {
            guard !reported else { return }
            for item in addedItems {
                if case .barcode(let barcode) = item, let code = barcode.payloadStringValue {
                    reported = true
                    dataScanner.stopScanning()
                    UINotificationFeedbackGenerator().notificationOccurred(.success)
                    onDetected(code)
                    return
                }
            }
        }
    }
}
