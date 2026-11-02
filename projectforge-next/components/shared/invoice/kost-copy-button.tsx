"use client";

import type { KeyboardEvent, MouseEvent } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Copy01Icon, TickDouble01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useCopyToClipboard } from "@/hooks/use-copy-to-clipboard";
import { cn } from "@/lib/utils";
import { kostDigits, type KostZuweisungValues } from "./kost-zuweisung";

/**
 * Puts a cost 1 or cost 2 unit on the clipboard as digits only (see [kostDigits]) — what other
 * accounting systems take, so the dots need not be deleted there by hand. Renders nothing without a
 * number.
 *
 * `inTrigger` for the folded row's summary, which sits inside the row's `CollapsibleTrigger`, i.e. inside
 * a button: a button there is invalid, so this is a `span` acting as one, and neither the click nor the
 * key that copies reaches the trigger — copying leaves the row folded (as [OrderLink] does there).
 * Otherwise a ghost icon button, beside the field of the open row.
 */
export function KostCopyButton({
  kost,
  kind,
  inTrigger,
  className,
}: {
  kost: KostZuweisungValues["kost1" | "kost2"] | undefined;
  /** Which of the two it is, for the accessible name. */
  kind: "kost1" | "kost2";
  inTrigger?: boolean;
  className?: string;
}) {
  const t = useTranslations();
  const { copied, copy } = useCopyToClipboard();
  const digits = kostDigits(kost);
  if (!digits) return null;

  // Spelled out, as the key scan of the i18n generator finds literal keys only.
  const label = copied
    ? t("copied")
    : kind === "kost1"
      ? t("fibu.kost1.copyWithoutDots")
      : t("fibu.kost2.copyWithoutDots");
  const icon = (
    <HugeiconsIcon
      icon={copied ? TickDouble01Icon : Copy01Icon}
      size={inTrigger ? 12 : 14}
    />
  );

  if (!inTrigger) {
    return (
      <HintTooltip text={label} plain>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          className={cn("shrink-0 text-muted-foreground", className)}
          aria-label={label}
          onClick={() => void copy(digits)}
        >
          {icon}
        </Button>
      </HintTooltip>
    );
  }

  const handle = (event: MouseEvent | KeyboardEvent) => {
    event.preventDefault();
    event.stopPropagation();
    void copy(digits);
  };
  return (
    <HintTooltip text={label} plain>
      <span
        role="button"
        tabIndex={0}
        aria-label={label}
        className={cn(
          "inline-flex shrink-0 cursor-pointer items-center rounded-sm p-0.5 align-middle text-muted-foreground",
          "hover:bg-muted hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none",
          className
        )}
        // The trigger opens on pointer down too (useCopyableTrigger tracks it), so that one stays here as
        // well.
        onPointerDown={(event) => event.stopPropagation()}
        onClick={handle}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") handle(event);
        }}
      >
        {icon}
      </span>
    </HintTooltip>
  );
}
