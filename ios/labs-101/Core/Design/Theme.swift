import SwiftUI
import UIKit

// the yard ops tokens of the web (frontend/app/globals.css), light and dark

extension Color {
    static let canvas = Color(light: 0xd6def6, dark: 0x1e2742)
    static let surfaceRaised = Color(light: 0xffffff, dark: 0x19213a)
    static let surfaceSunken = Color(light: 0xeef1f7, dark: 0x0f1526)
    static let line = Color(light: 0xe3e7f0, dark: 0x2b3552)
    static let track = Color(light: 0xdfe4ee, dark: 0x2f3a58)
    static let ink = Color(light: 0x0f1424, dark: 0xeef1f8)
    static let inkMuted = Color(light: 0x5f677a, dark: 0xa3acc2)
    static let brand = Color(light: 0x3560e0, dark: 0x6f8ff7)
    static let brandStrong = Color(light: 0x2747c2, dark: 0x9db2ff)
    static let brandSoft = Color(light: 0xe6ecfd, dark: 0x283a7a)
    static let onBrand = Color(light: 0xffffff, dark: 0x0b1020)
    static let success = Color(light: 0x23763c, dark: 0x6fd38b)
    static let successSoft = Color(light: 0xdef3e5, dark: 0x163824)
    static let successBar = Color(light: 0x338548, dark: 0x4fbf6c)
    static let warning = Color(light: 0x9a5208, dark: 0xf2ae5c)
    static let warningSoft = Color(light: 0xfdf0d6, dark: 0x3b2a10)
    static let danger = Color(light: 0xc42d2d, dark: 0xf47a7a)
    static let dangerSoft = Color(light: 0xfde4e4, dark: 0x3f1a1c)

    /// categorical series for charts with several series
    static let series1 = Color(light: 0x3560e0, dark: 0x5b7cf0)
    static let series2 = Color(light: 0xc4721f, dark: 0xc97d2c)
    static let series3 = Color(light: 0x13917f, dark: 0x2a9d8a)
    static let series4 = Color(light: 0x8b5cf6, dark: 0x8f74e6)

    /// sequential blue ramp for ordered levels (heart rate zones, sleep depth), light to strong
    static let ramp1 = Color(light: 0xb9c8f5, dark: 0x2c3d7a)
    static let ramp2 = Color(light: 0x8aa3ef, dark: 0x3f57b0)
    static let ramp3 = Color(light: 0x5f80e8, dark: 0x5b7cf0)
    static let ramp4 = Color(light: 0x3560e0, dark: 0x8aa3f5)
    static let ramp5 = Color(light: 0x2240a8, dark: 0xb9c8fb)

    // the soft colour fields behind the glass
    fileprivate static let field1 = Color(light: 0x3560e0, lightAlpha: 0.30, dark: 0x4f70f0, darkAlpha: 0.34)
    fileprivate static let field2 = Color(light: 0x8b78f0, lightAlpha: 0.24, dark: 0x7c62e6, darkAlpha: 0.26)
    fileprivate static let field3 = Color(light: 0x60c4ec, lightAlpha: 0.26, dark: 0x38a0d2, darkAlpha: 0.20)

    // liquid glass fill, brighter at the top left like on the web
    fileprivate static let glassTop = Color(light: 0xffffff, lightAlpha: 0.72, dark: 0x28345c, darkAlpha: 0.62)
    fileprivate static let glassBottom = Color(light: 0xffffff, lightAlpha: 0.42, dark: 0x19213a, darkAlpha: 0.42)

    init(light: UInt32, lightAlpha: CGFloat = 1, dark: UInt32, darkAlpha: CGFloat = 1) {
        self.init(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark
                ? UIColor(hex: dark, alpha: darkAlpha)
                : UIColor(hex: light, alpha: lightAlpha)
        })
    }
}

private extension UIColor {
    convenience init(hex: UInt32, alpha: CGFloat) {
        self.init(red: CGFloat((hex >> 16) & 0xff) / 255,
                  green: CGFloat((hex >> 8) & 0xff) / 255,
                  blue: CGFloat(hex & 0xff) / 255,
                  alpha: alpha)
    }
}

// MARK: Backdrop and glass

/// The canvas with three soft colour fields, the glass panes in front of it refract them.
struct Backdrop: View {
    var body: some View {
        GeometryReader { proxy in
            let size = proxy.size
            ZStack {
                Color.canvas
                field(.field1, at: UnitPoint(x: 0.08, y: 0.04), radius: size.width * 1.1, in: size)
                field(.field2, at: UnitPoint(x: 0.92, y: 0.18), radius: size.width * 0.95, in: size)
                field(.field3, at: UnitPoint(x: 0.6, y: 1), radius: size.width * 1.15, in: size)
            }
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }

    private func field(_ color: Color, at center: UnitPoint, radius: CGFloat, in size: CGSize) -> some View {
        RadialGradient(colors: [color, color.opacity(0)], center: center, startRadius: 0, endRadius: radius * 0.7)
    }
}

/// A pane of liquid glass as a fill, for list rows and cards.
struct GlassFill: View {
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency

    var body: some View {
        if reduceTransparency {
            Color.surfaceRaised
        } else {
            ZStack {
                Rectangle().fill(.ultraThinMaterial)
                LinearGradient(colors: [.glassTop, .glassBottom], startPoint: .topLeading, endPoint: .bottomTrailing)
            }
        }
    }
}

extension View {
    /// A list or form on the colour fields, its rows become glass. Apply to the `List`/`Form`.
    func glassList() -> some View {
        scrollContentBackground(.hidden)
            .background { Backdrop() }
    }

    /// Glass row backgrounds for every row inside, apply to the content of a `List`/`Form`.
    func glassRows() -> some View {
        listRowBackground(GlassFill())
    }
}

// MARK: Building blocks

/// A glyph in a soft brand tile, `solid` fills it with the brand for the selected or running object.
struct IconTile: View {
    let systemImage: String
    var solid = false
    var size: CGFloat = 36

    var body: some View {
        Image(systemName: systemImage)
            .font(.system(size: size * 0.45, weight: .semibold))
            .foregroundStyle(solid ? Color.onBrand : Color.brand)
            .frame(width: size, height: size)
            .background(solid ? Color.brand : Color.brandSoft, in: .rect(cornerRadius: size * 0.28))
            .accessibilityHidden(true)
    }
}

/// A thin progress bar on the track, green once the goal is reached.
struct GoalBar: View {
    let value: Double
    let goal: Double
    var color: Color = .brand

    var body: some View {
        let fraction = goal > 0 ? min(1, max(0, value / goal)) : 0
        GeometryReader { proxy in
            Capsule()
                .fill(Color.track)
                .overlay(alignment: .leading) {
                    Capsule()
                        .fill(value >= goal && goal > 0 ? Color.successBar : color)
                        .frame(width: proxy.size.width * fraction)
                }
        }
        .frame(height: 6)
        .accessibilityHidden(true)
    }
}
