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
 * which the list doesn't show unless one has access to it — and, where external access isn't allowed here
 * (gateway mode), the note that areas with external access are administered on the other server.
 */
export function DataTransferListActions() {
  const t = useTranslations();
  const options = useDataTransferOptions();

  return (
    <>
      {options && !options.externalAccessAllowed && (
        <HintTooltip
          text={t("plugins.datatransfer.gateway.externalAccess.list", {
            arg0: options.gatewayHost,
          })}
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
      <HintTooltip text={t("plugins.datatransfer.personalBox.info")}>
        <Button asChild variant="ghost" size="sm" className="gap-1.5">
          <GuardedLink href="/datatransfer/personal-box">
            <HugeiconsIcon icon={UserIcon} size={14} aria-hidden />
            {t("plugins.datatransfer.personalBox._")}
          </GuardedLink>
        </Button>
      </HintTooltip>
    </>
  );
}
