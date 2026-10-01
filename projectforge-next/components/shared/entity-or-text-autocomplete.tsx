"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  Building03Icon,
  Cancel01Icon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { cn } from "@/lib/utils";
import type { EntityRef } from "./entity-autocomplete";
import { EntitySearchList } from "./entity-search-list";
import { FreeTextBadge } from "./free-text-badge";
import { useTypeToOpen } from "./use-type-to-open";

/** Either a picked record or a typed text — never both. */
export type EntityOrText =
  | { kind: "entity"; ref: EntityRef }
  | { kind: "text"; text: string };

export interface EntityOrTextAutocompleteProps {
  /** The lookup url, with its literal `:search` placeholder (see [EntityAutocomplete]). */
  url: string;
  value: EntityOrText | null;
  onChange: (value: EntityOrText | null) => void;
  /** The longest free text the field takes; a longer term is offered but cannot be picked. */
  maxLength?: number;
  id?: string;
  /** Accessible name of the trigger; the kind of the value is added to it. */
  "aria-label"?: string;
  className?: string;
  disabled?: boolean;
}

/**
 * Picks a record — or, where none fits, takes the typed term as free text: the customer of an order or
 * an invoice, which is either a customer of the list or a name typed for one that has no record.
 *
 * Which of the two the value is shows at a glance: a record with the customer icon, a free text with
 * the [FreeTextBadge], in italics, and with a dashed warning border — so a free text cannot pass for a
 * customer that was meant to be picked. The free text is offered last in the dropdown (see
 * [EntitySearchFreeText]), so Enter takes a matching record.
 *
 * Opened on a free text, the search starts with that text: the records named like it are offered at
 * once, which is how a free text is turned into the customer it should have been.
 */
export function EntityOrTextAutocomplete({
  url,
  value,
  onChange,
  maxLength,
  id,
  className,
  disabled,
  "aria-label": ariaLabel,
}: EntityOrTextAutocompleteProps) {
  const t = useTranslations();
  const { open, onOpenChange, initialSearch, openOnTyping } = useTypeToOpen({
    initiallyOpen: false,
    disabled,
  });
  const isText = value?.kind === "text";
  const shown = !value
    ? null
    : value.kind === "entity"
      ? value.ref.displayName
      : value.text;
  const kindLabel = isText ? t("fibu.kunde.freeText._") : undefined;

  function pick(next: EntityOrText | null) {
    onChange(next);
    onOpenChange(false);
  }

  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <div className={cn("flex min-w-0 items-center gap-1", className)}>
        <PopoverTrigger asChild>
          <Button
            id={id}
            type="button"
            variant="outline"
            role="combobox"
            aria-expanded={open}
            aria-label={
              ariaLabel &&
              [ariaLabel, kindLabel, shown].filter(Boolean).join(": ")
            }
            disabled={disabled}
            onKeyDown={openOnTyping}
            data-value-kind={value?.kind}
            className={cn(
              "h-7 min-w-0 flex-1 justify-between gap-1.5 px-2 text-xs font-normal",
              isText && "border-dashed border-warning"
            )}
          >
            <span className="flex min-w-0 items-center gap-1.5">
              {value?.kind === "entity" && (
                <HugeiconsIcon
                  icon={Building03Icon}
                  size={14}
                  className="shrink-0 text-muted-foreground"
                  aria-hidden
                />
              )}
              {isText && <FreeTextBadge />}
              <span
                className={cn(
                  "truncate",
                  !value && "text-muted-foreground",
                  // `pr-0.5`: an italic glyph leans past its box, and `truncate` clips the overhang (the last "l").
                  isText && "pr-0.5 italic"
                )}
              >
                {shown ?? t("filter.chooseEntity")}
              </span>
            </span>
            <HugeiconsIcon icon={ArrowDown01Icon} size={14} aria-hidden />
          </Button>
        </PopoverTrigger>
        {value && !disabled && (
          <button
            type="button"
            // On pointer down, not click — see EntityAutocomplete.
            onPointerDown={(e) => {
              e.preventDefault();
              onChange(null);
            }}
            aria-label={`${t("reset")}: ${ariaLabel ?? shown}`}
            className="shrink-0 cursor-pointer text-muted-foreground hover:text-foreground"
          >
            <HugeiconsIcon icon={Cancel01Icon} size={12} />
          </button>
        )}
      </div>
      <PopoverContent
        align="start"
        className="w-(--radix-popover-trigger-width) min-w-56 p-0"
      >
        <EntitySearchList
          url={url}
          active={open}
          initialSearch={initialSearch || (isText ? value.text : "")}
          onPick={(ref) => pick({ kind: "entity", ref })}
          freeText={{
            heading: t("fibu.kunde.freeText._"),
            itemLabel: (term) => t("fibu.kunde.freeText.use", { arg0: term }),
            tooLongLabel: (max) =>
              t("fibu.kunde.freeText.tooLong", { arg0: max }),
            maxLength,
            onPick: (text) => pick({ kind: "text", text }),
          }}
        />
      </PopoverContent>
    </Popover>
  );
}
