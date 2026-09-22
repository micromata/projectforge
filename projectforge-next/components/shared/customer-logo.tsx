"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { useSystemStatus } from "@/hooks/use-system-status";

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

  if (!logoUrl || failed) return null;
  const src = (url: string) => `/rsPublic/${url.replace(/^\/+/, "")}`;
  const hasDark = !!logoUrlDark && !failedDark;

  return (
    <>
      {/* eslint-disable-next-line @next/next/no-img-element -- a runtime backend url of unknown size, and
          next/image cannot be used with output: "export" here (see LogoRow). */}
      <img
        src={src(logoUrl)}
        alt={t("customer")}
        // Bounds an arbitrary customer image inside the row's fixed height - without it a tall logo
        // would push the row open and the collapse arithmetic with it. w-auto keeps the aspect ratio.
        className={cn("max-h-9 w-auto", hasDark && "dark:hidden")}
        onError={() => setFailed(true)}
      />
      {hasDark && (
        // eslint-disable-next-line @next/next/no-img-element -- see above.
        <img
          src={src(logoUrlDark!)}
          alt={t("customer")}
          className="hidden max-h-9 w-auto dark:block"
          onError={() => setFailedDark(true)}
        />
      )}
    </>
  );
}
