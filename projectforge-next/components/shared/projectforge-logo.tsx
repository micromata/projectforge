import { cn } from "@/lib/utils";
// The flame is a fixed red vector (fill baked into the asset), so it reads on a light and a dark
// background alike and needs no theme variant. Static import rather than public/: next rewrites it to
// /next/_next/static/media/<name>.<hash>.svg, so the url carries the basePath in dev and in the static
// export both (see the note in logo-row.tsx). It returns a StaticImageData, hence `.src`.
import flame from "./projectforge-flame.svg";

interface ProjectForgeLogoProps {
  /** Accessible name of the wordmark. Exposed as a single `img` role, so the rebuilt text and the
   *  flame read as one labelled mark rather than as loose glyphs (see logo-row's e2e locator). */
  label: string;
  className?: string;
}

/**
 * The ProjectForge wordmark, rebuilt from parts so it follows the theme: the "Project"/"forge" text is
 * drawn in `currentColor` (foreground), the flame kept in its brand red. This replaces the old raster
 * (grey text on transparent), which needed a white plate to stay legible in dark mode.
 */
export function ProjectForgeLogo({ label, className }: ProjectForgeLogoProps) {
  return (
    // A diagonal cascade like the original: "Project" top-left, the flames top-right, "Forge" below
    // and shifted right so it sits under the flames. pr reserves the right column the flames are
    // absolutely placed into; the text stays a real, theme-coloured two-line block.
    <div
      role="img"
      aria-label={label}
      className={cn(
        "relative inline-flex flex-col items-start pr-8 text-foreground",
        "font-semibold italic leading-[1.05] tracking-tight",
        className
      )}
    >
      <span className="text-[15px]">Project</span>
      <span className="pl-3 text-[15px]">Forge</span>
      {/* Decorative: the wordmark text already names the app (see the row's aria-label). */}
      {/* eslint-disable-next-line @next/next/no-img-element -- a static vector asset, next/image is
          incompatible with output: "export" here (see logo-row.tsx). */}
      <img
        src={flame.src}
        alt=""
        aria-hidden
        // Sheared to match the italic lean of the text, as in the original mark.
        className="pointer-events-none absolute -right-0.5 -top-1 h-11 w-9 -skew-x-12"
      />
    </div>
  );
}
