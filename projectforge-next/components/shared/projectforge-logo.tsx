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
        // self-end anchors the mark at the bottom of the (overflow-hidden) brand row so the tall
        // flames get clearance at the top instead of being clipped there.
        "relative inline-flex flex-col items-start self-end pb-1 pr-8 text-foreground",
        // Bold italic narrow sans (Archivo Narrow, Roboto Condensed fallback - see .wordmark-font in
        // globals.css and the fonts in app/layout.tsx), tight tracking and leading, to approximate the
        // original logo lettering.
        "wordmark-font font-bold italic leading-[0.85] tracking-[-0.04em]",
        className
      )}
    >
      <span className="translate-x-2 text-[13px]">Project</span>
      {/* Slid right with a transform (not layout) so its tail runs under the flames, as in the
          original, without widening the box and pushing the flames out with it. */}
      <span className="translate-x-4 pl-3 text-[13px]">Forge</span>
      {/* Decorative: the wordmark text already names the app (see the row's aria-label). */}
      {/* eslint-disable-next-line @next/next/no-img-element -- a static vector asset, next/image is
          incompatible with output: "export" here (see logo-row.tsx). */}
      <img
        src={flame.src}
        alt=""
        aria-hidden
        // Sheared to match the italic lean of the text, as in the original mark. Anchored at the
        // bottom and made tall so the top rises above "Project" while the lower end sits halfway onto
        // "Forge". z-10 keeps the flames above "Forge", whose transform would otherwise paint over them.
        className="pointer-events-none absolute bottom-2.5 right-0 z-10 h-9 w-7 -skew-x-12"
      />
    </div>
  );
}
