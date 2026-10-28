"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Badge } from "@/components/ui/badge";

/**
 * Marks a chat made from a data transfer area (DataTransferChatHost): a pill "Data transfer" that leads to the
 * area. Not a link inside another link: beside the title in the lists, not in it.
 */
export function ChatDataTransferPill({ link }: { link: string | null }) {
  const t = useTranslations();
  const label = t("chat.dataTransfer._");
  if (!link) {
    return (
      <Badge variant="secondary" className="shrink-0">
        {label}
      </Badge>
    );
  }
  return (
    <HintTooltip text={t("chat.dataTransfer.open")}>
      <Badge asChild variant="secondary" className="shrink-0">
        <Link href={link}>{label}</Link>
      </Badge>
    </HintTooltip>
  );
}
