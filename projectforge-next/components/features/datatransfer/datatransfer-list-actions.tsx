"use client";

import { GuardedLink } from "@/components/shared/guarded-link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { InformationCircleIcon, UserIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useDataTransferOptions } from "./use-datatransfer-options";

/**
 * The toolbar of the area list: the way to another user's personal box — the box one sends a file to,
 * which the list doesn't show unless one has access to it — and, where external access isn't allowed here,
 * the note that it can't be configured.
 *
 * The gateway has no personal boxes (only the areas pushed by the main instance), so no way to them there,
 * but the note that the areas are administered in the main instance instead.
 */
export function DataTransferListActions() {
  const t = useTranslations();
  const options = useDataTransferOptions();

  return (
    <>
      {options && !options.externalAccessAllowed && (
        <HintTooltip
          text={t("plugins.datatransfer.gateway.externalAccess.list")}
          openOnTap
        >
          <span
            className="inline-flex items-center text-muted-foreground"
            tabIndex={0}
            aria-label={t("plugins.datatransfer.external.access.title")}
          >
            <HugeiconsIcon icon={InformationCircleIcon} size={16} aria-hidden />
          </span>
        </HintTooltip>
      )}
      {options?.gatewayMode && (
        <HintTooltip
          text={t("plugins.datatransfer.gateway.listManagedByMainInstance")}
          openOnTap
        >
          <span
            className="inline-flex items-center text-muted-foreground"
            tabIndex={0}
            aria-label={t("plugins.datatransfer.gateway.listManagedByMainInstance")}
          >
            <HugeiconsIcon icon={InformationCircleIcon} size={16} aria-hidden />
          </span>
        </HintTooltip>
      )}
      {options && !options.gatewayMode && (
        <HintTooltip text={t("plugins.datatransfer.personalBox.info")}>
          <Button asChild variant="ghost" size="sm" className="gap-1.5">
            <GuardedLink href="/datatransfer/personal-box">
              <HugeiconsIcon icon={UserIcon} size={14} aria-hidden />
              {t("plugins.datatransfer.personalBox._")}
            </GuardedLink>
          </Button>
        </HintTooltip>
      )}
    </>
  );
}
