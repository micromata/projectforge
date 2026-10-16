"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import type { ParsedAddressField } from "@/lib/rs/address";
import { cn } from "@/lib/utils";

interface Props {
  field: string;
  offer: ParsedAddressField;
  checked: boolean;
  onToggle: () => void;
  /** The field's name, or a select moving it to another field. */
  label: ReactNode;
}

const CONFIDENCE_CLASS: Record<string, string> = {
  high: "bg-brand-green/15 text-brand-green-dark",
  medium: "bg-warning/15 text-warning",
  low: "bg-destructive/10 text-destructive",
};

/**
 * One offered field: the box to tick it, its name, and its value — next to the one it replaces, struck through,
 * where the form has another (the old → new of the import diff). A blank value empties the field. The confidence
 * of the text parser, if any, follows as a badge.
 */
export function AddressOfferRow({
  field,
  offer,
  checked,
  onToggle,
  label,
}: Props) {
  const t = useTranslations();
  const id = `address-offer-${field}`;
  const current = offer.currentValue?.trim();
  const confidence = offer.confidence?.toLowerCase();
  return (
    <li className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
      <Checkbox
        id={id}
        checked={checked}
        onCheckedChange={onToggle}
        aria-label={typeof label === "string" ? label : field}
      />
      <span className="min-w-40 font-medium">
        {typeof label === "string" ? (
          <label htmlFor={id}>{label}</label>
        ) : (
          label
        )}
      </span>
      <span className="flex min-w-0 flex-1 flex-wrap items-center gap-2">
        {current && current !== offer.value && (
          <span className="break-all text-brand-pink line-through">
            {current}
          </span>
        )}
        <span
          className={cn(
            "break-all",
            current !== offer.value && "text-brand-green-dark"
          )}
        >
          {offer.value || "—"}
        </span>
        {confidence && CONFIDENCE_CLASS[confidence] && (
          <HintTooltip text={confidenceText(t, confidence)} openOnTap>
            <Badge className={CONFIDENCE_CLASS[confidence]}>{confidence}</Badge>
          </HintTooltip>
        )}
      </span>
    </li>
  );
}

/** Literal keys, so the i18n generator finds them. */
function confidenceText(
  t: ReturnType<typeof useTranslations>,
  confidence: string
): string {
  if (confidence === "high") return t("address.parseText.confidence.high");
  if (confidence === "medium") return t("address.parseText.confidence.medium");
  return t("address.parseText.confidence.low");
}
