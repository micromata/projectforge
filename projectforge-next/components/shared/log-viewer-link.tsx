"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { LeftToRightListBulletIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";

/**
 * "View log": opens the log viewer with a log subscription of the user (e.g. `next/logViewer/7`, see the
 * backend's `LogViewerRest.viewerUrl`) in a new tab, so the page it is called from stays as it is.
 *
 * The url comes from the page's own endpoint, which also registers the subscription; nothing is rendered until
 * it is there. `label` and `hint` for a link of another kind beside it (the problem dashboard's log file search).
 */
export function LogViewerLink({
  url,
  label,
  hint,
}: {
  url: string | null | undefined;
  label?: string;
  hint?: string;
}) {
  const t = useTranslations();
  if (!url) return null;
  return (
    <HintTooltip text={hint}>
      <Button asChild size="sm" variant="outline">
        <a
          href={toAbsoluteUrl(resolveMenuUrl(url))}
          target="_blank"
          rel="noopener"
        >
          <HugeiconsIcon icon={LeftToRightListBulletIcon} size={13} />
          {label ?? t("system.admin.logViewer.title")}
        </a>
      </Button>
    </HintTooltip>
  );
}
