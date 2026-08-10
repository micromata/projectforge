"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { CircleArrowReload01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import { cn } from "@/lib/utils";

/**
 * The way back to the legacy version of the page at hand — the escape hatch of the migration: a
 * migrated page replaces its React counterpart everywhere (menu and all server side redirects, see
 * `NextMigration`), so without this the user is stuck here if the new page has a gap.
 *
 * The url comes from the server (`UILayout.legacyUrl` / `InitialListData.legacyEditPage`), because
 * it isn't derivable here: `books` is not `book`, and pages whose rows open something other than an
 * edit form point somewhere else entirely.
 *
 * Rendered as a plain anchor, not `next/link`: the target belongs to another app and needs a full
 * page load — which is exactly what `resolveMenuUrl` decides.
 */
export function LegacyPageLink({
  url,
  className,
}: {
  /** Nothing is rendered without one: pages with no legacy counterpart (login, 2FA) have none. */
  url?: string;
  className?: string;
}) {
  const t = useTranslations("goreact.menu");
  if (!url) return null;
  const label = t("classics");

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button
          asChild
          variant="ghost"
          size="icon"
          aria-label={label}
          className={cn(
            "size-7 shrink-0 text-muted-foreground hover:text-foreground",
            className
          )}
        >
          <a href={toAbsoluteUrl(resolveMenuUrl(url))}>
            <HugeiconsIcon icon={CircleArrowReload01Icon} size={15} />
          </a>
        </Button>
      </TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  );
}
