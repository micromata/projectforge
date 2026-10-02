"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { LeftToRightListBulletIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { fetchDatevImportLogViewerUrl } from "@/lib/rs/datev-import";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";

/**
 * "View log" of the DATEV import, as the legacy Wicket page offers it: opens the (React) log viewer with the
 * user's import log (upload, faulty rows, reconcile, commit progress) in a new tab, so the import page with its
 * preview or running job stays as it is.
 *
 * The url is fetched on mount, which also registers the log subscription early; the backend ensures it on every
 * import request anyway, so nothing is lost if this request comes late.
 */
export function DatevImportLogViewerLink() {
  const t = useTranslations();
  const { data: url } = useQuery({
    queryKey: ["datev-import", "logViewer"],
    queryFn: ({ signal }) => fetchDatevImportLogViewerUrl(signal),
    staleTime: Infinity,
  });
  if (!url) return null;
  return (
    <Button asChild size="sm" variant="outline">
      <a
        href={toAbsoluteUrl(resolveMenuUrl(url))}
        target="_blank"
        rel="noopener"
      >
        <HugeiconsIcon icon={LeftToRightListBulletIcon} size={13} />
        {t("system.admin.logViewer.title")}
      </a>
    </Button>
  );
}
