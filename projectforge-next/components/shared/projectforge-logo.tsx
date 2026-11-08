import { cn } from "@/lib/utils";
// The wordmark (flames + "ProjectForge") in two variants, dark text for the light theme and white text for
// the dark one, rendered from the master SVGs (dev-docs/logo/render-logos.py). Static imports rather than
// public/: next rewrites them to /next/_next/static/media/<name>.<hash>.svg, so the url carries the basePath
// in dev and in the static export both (see the note in logo-row.tsx). They return a StaticImageData,
// hence `.src`.
import logoLight from "./projectforge-logo-light.svg";
import logoDark from "./projectforge-logo-dark.svg";

interface ProjectForgeLogoProps {
  /** Accessible name of the wordmark. Exposed as a single `img` role, so the two variants read as one
   *  labelled mark (see logo-row's e2e locator). */
  label: string;
  className?: string;
}

/**
 * The ProjectForge wordmark. The variant is swapped by CSS (`dark:`), as CustomerLogo does: next-themes
 * sets `.dark` before paint, where a `useTheme()` pick would flicker in the static export.
 */
export function ProjectForgeLogo({ label, className }: ProjectForgeLogoProps) {
  return (
    <div role="img" aria-label={label} className={cn("inline-flex", className)}>
      {/* Decorative: the wrapper names the app. */}
      {/* eslint-disable-next-line @next/next/no-img-element -- a static vector asset, next/image is
          incompatible with output: "export" here (see logo-row.tsx). */}
      <img
        src={logoLight.src}
        alt=""
        aria-hidden
        className="h-8 w-auto dark:hidden"
      />
      {/* eslint-disable-next-line @next/next/no-img-element -- as above. */}
      <img
        src={logoDark.src}
        alt=""
        aria-hidden
        className="hidden h-8 w-auto dark:block"
      />
    </div>
  );
}
