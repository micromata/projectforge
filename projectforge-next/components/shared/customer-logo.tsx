"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useSystemStatus } from "@/hooks/use-system-status";

/**
 * The customer's own logo, where one is configured (`SystemStatus.logoUrl`, a bare file name such as
 * "logo.png" served by LogoServiceRest under /rsPublic).
 *
 * The url is root-relative and deliberately carries no BASE_PATH: Spring serves /rsPublic at the
 * origin root, not under /next (see lib/config.ts). In dev next.config.ts proxies it to :8080.
 */
export function CustomerLogo() {
  const t = useTranslations("logo");
  const { data } = useSystemStatus();
  const logoUrl = data?.logoUrl;
  // A logo that is configured but unreadable answers 500 rather than 404 (LogoServiceRest throws),
  // so the url alone cannot say whether there is an image behind it. The browser can - so let it, and
  // drop the element instead of leaving a torn-image icon in the header.
  const [failed, setFailed] = useState(false);

  if (!logoUrl || failed) return null;
  return (
    // A runtime backend url of unknown size, so next/image has nothing to work with here - and it
    // cannot be used in this app at all, see the note in LogoRow.
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={`/rsPublic/${logoUrl.replace(/^\/+/, "")}`}
      alt={t("customer")}
      // Bounds an arbitrary customer image inside the row's fixed height - without it a tall logo
      // would push the row open and the collapse arithmetic with it. w-auto keeps the aspect ratio.
      className="max-h-9 w-auto"
      onError={() => setFailed(true)}
    />
  );
}
