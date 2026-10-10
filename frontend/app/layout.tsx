import type { Metadata } from "next";
import { Geist, Geist_Mono, Inter } from "next/font/google";
import "./globals.css";
import { cn } from "@/lib/utils";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AppNavbar } from "@/components/app-navbar";
import { ThemeProvider } from "@/components/theme-provider";
import { UserProvider } from "@/components/user-provider";
import { APP_URL, getUser } from "@/lib/auth";

const inter = Inter({ subsets: ['latin'], variable: '--font-sans' });

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const description = "Ernährung, Training und Laufen an einem Ort: Mahlzeiten tracken, Workouts planen und Health-Daten synchronisieren.";

export const metadata: Metadata = {
  metadataBase: new URL(APP_URL),
  title: { default: "Labs-101", template: "%s · Labs-101" },
  description,
  applicationName: "Labs-101",
  // the image itself comes from app/opengraph-image.png
  openGraph: {
    type: "website",
    siteName: "Labs-101",
    locale: "de_DE",
    title: "Labs-101",
    description,
  },
  twitter: {
    card: "summary_large_image",
    title: "Labs-101",
    description,
  },
};

export default async function RootLayout({ children }: LayoutProps<"/">) {
  const user = await getUser()

  return (
    <html
      lang="de"
      suppressHydrationWarning
      className={cn("h-full antialiased font-sans", inter.variable, geistMono.variable)}
    >
      <body className="min-h-full flex flex-col">
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          <UserProvider user={user}>
            <AppNavbar />
            <main className="flex-1 min-w-0 flex flex-col">
              {children}
            </main>
          </UserProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
