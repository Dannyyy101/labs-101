import SwiftUI

// the dark yard ops tokens of the web (frontend/app/globals.css), the watch is always dark
extension Color {
    static let brand = Color(hex: 0x6f8ff7)
    static let brandStrong = Color(hex: 0x9db2ff)
    static let brandSoft = Color(hex: 0x283a7a)
    static let onBrand = Color(hex: 0x0b1020)
    static let success = Color(hex: 0x6fd38b)
    static let successBar = Color(hex: 0x4fbf6c)

    /// the colour field behind the pages, like the soft fields behind the glass on the web
    static let field = Color(hex: 0x4f70f0).opacity(0.45)

    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xff) / 255,
                  green: Double((hex >> 8) & 0xff) / 255,
                  blue: Double(hex & 0xff) / 255)
    }
}

/// The colour field behind the screens outside of a running workout.
struct WatchBackdrop: View {
    var body: some View {
        LinearGradient(colors: [.field, .clear], startPoint: .top, endPoint: .bottom)
            .ignoresSafeArea()
            .accessibilityHidden(true)
    }
}
