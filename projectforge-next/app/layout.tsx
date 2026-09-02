import type { Metadata } from "next";
import {
  Archivo_Narrow,
  Geist_Mono,
  Plus_Jakarta_Sans,
  Roboto_Condensed,
} from "next/font/google";
import { ThemeProvider } from "next-themes";
import { LocaleProvider } from "@/i18n/locale-provider";
import { DEFAULT_LOCALE } from "@/i18n/config";
import { Toaster } from "sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryProvider } from "@/lib/query-client";
import "./globals.css";
import { cn } from "@/lib/utils";

const jakarta = Plus_Jakarta_Sans({
  subsets: ["latin"],
  variable: "--font-sans",
  weight: ["300", "400", "500", "600", "700"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

// Only for the ProjectForge wordmark (see ProjectForgeLogo): a bold, italic narrow sans that
// approximates the original logo lettering, with Roboto Condensed as the documented fallback (the
// wordmark chains both variables, see the .wordmark-font rule in globals.css).
const archivoNarrow = Archivo_Narrow({
  variable: "--font-condensed",
  subsets: ["latin"],
  weight: ["700"],
  style: ["italic"],
});

const robotoCondensed = Roboto_Condensed({
  variable: "--font-condensed-fallback",
  subsets: ["latin"],
  weight: ["700"],
  style: ["italic"],
});

export const metadata: Metadata = {
  title: "ProjectForge",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      // The static export prerenders with the default locale; LocaleProvider
      // sets the real one (and this lang attribute) on the client.
      lang={DEFAULT_LOCALE}
      suppressHydrationWarning
      className={cn(
        "h-full",
        jakarta.variable,
        geistMono.variable,
        archivoNarrow.variable,
        robotoCondensed.variable,
        "font-sans"
      )}
    >
      <body className="min-h-full flex flex-col">
        <ThemeProvider attribute="class" defaultTheme="system" enableSystem>
          {/* QueryProvider wraps LocaleProvider because the latter fetches the deployment's
              CustomerI18nResources overrides via TanStack Query to overlay on its static catalog. */}
          <QueryProvider>
            <LocaleProvider>
              <TooltipProvider>
                {children}
                {/* More than sonner's three: a job's progress toast stays until the job is done
                    (see JobToasts) and must not be pushed out by the messages of other actions.
                    `pointer-events-auto` on each toast keeps it clickable over a modal dialog: an open
                    Radix modal sets `body { pointer-events: none }` (react-dismissable-layer), which
                    would otherwise swallow the toast's close button and let the click fall through to
                    the overlay behind it — closing the dialog while the toast stayed (see
                    EntityEditModal's onInteractOutside, which then keeps the dialog open). */}
                <Toaster
                  richColors
                  position="top-right"
                  visibleToasts={5}
                  toastOptions={{ className: "pointer-events-auto" }}
                />
              </TooltipProvider>
            </LocaleProvider>
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
