"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { useSystemStatus } from "@/hooks/use-system-status";
import { type TrimmedImage, useTrimmedImage } from "@/hooks/use-trimmed-image";

/** max-h-9: the logo row is h-12, see LogoRow. */
const MAX_HEIGHT = 36;

/**
 * The customer's own logo, where one is configured (`SystemStatus.logoUrl`, a bare file name such as
 * "logo.png" served by LogoServiceRest under /rsPublic).
 *
 * An optional dark variant (`SystemStatus.logoUrlDark`, e.g. "logoDark.png") is swapped in via CSS rather
 * than by reading the theme: the dark logo has no vector/currentColor hook, so the admin supplies a second
 * image. CSS keeps the swap flash-free with output: "export" (next-themes sets `.dark` before paint), where
 * a `useTheme()` pick would flicker. Without a dark variant the single logo is shown on both themes, as before.
 *
 * The url is root-relative and deliberately carries no BASE_PATH: Spring serves /rsPublic at the
 * origin root, not under /next (see lib/config.ts). In dev next.config.ts proxies it to :8080.
 *
 * A transparent margin around the image is cut off (useTrimmedImage): an admin's export often has one,
 * and the logo would then sit indented against the menu below. The visible part keeps the size it had in
 * the whole image, the row centres it vertically.
 */
export function CustomerLogo() {
  const t = useTranslations("logo");
  const { data } = useSystemStatus();
  const logoUrl = data?.logoUrl;
  const logoUrlDark = data?.logoUrlDark;
  // A logo that is configured but unreadable answers 500 rather than 404 (LogoServiceRest throws),
  // so the url alone cannot say whether there is an image behind it. The browser can - so let it, and
  // drop the element instead of leaving a torn-image icon in the header.
  const [failed, setFailed] = useState(false);
  // A broken dark variant falls back to showing the light logo on both themes rather than a torn image.
  const [failedDark, setFailedDark] = useState(false);
  const src = (url: string | undefined) =>
    url && `/rsPublic/${url.replace(/^\/+/, "")}`;
  // `projectforge.logoHeight`: the logo is shown at exactly that height; otherwise at its own, up to MAX_HEIGHT.
  const configuredHeight = data?.logoHeight;
  const light = useTrimmedImage(
    src(logoUrl),
    configuredHeight ?? MAX_HEIGHT,
    configuredHeight !== undefined
  );
  const dark = useTrimmedImage(
    src(logoUrlDark),
    configuredHeight ?? MAX_HEIGHT,
    configuredHeight !== undefined
  );

  if (!light || failed) return null;
  const hasDark = !!dark && !failedDark;
  // A crop is shown at the height its pixels had in the whole image, not stretched to max-h-9.
  const logoHeight = (image: TrimmedImage) => image.height ?? configuredHeight;
  const height = (image: TrimmedImage) =>
    logoHeight(image) === undefined
      ? undefined
      : ({ "--logo-height": `${logoHeight(image)}px` } as React.CSSProperties);

  return (
    <>
      {/* eslint-disable-next-line @next/next/no-img-element -- a runtime backend url of unknown size, and
          next/image cannot be used with output: "export" here (see LogoRow). */}
      <img
        src={light.src}
        alt={t("customer")}
        style={height(light)}
        // Bounds an arbitrary customer image inside the row's fixed height - without it a tall logo
        // would push the row open and the collapse arithmetic with it. w-auto keeps the aspect ratio.
        className={cn(
          "w-auto",
          configuredHeight === undefined && "max-h-9",
          logoHeight(light) !== undefined && "h-(--logo-height)",
          hasDark && "dark:hidden"
        )}
        onError={() => setFailed(true)}
      />
      {hasDark && (
        // eslint-disable-next-line @next/next/no-img-element -- see above.
        <img
          src={dark.src}
          alt={t("customer")}
          style={height(dark)}
          className={cn(
            "hidden w-auto dark:block",
            configuredHeight === undefined && "max-h-9",
            logoHeight(dark) !== undefined && "h-(--logo-height)"
          )}
          onError={() => setFailedDark(true)}
        />
      )}
    </>
  );
}
