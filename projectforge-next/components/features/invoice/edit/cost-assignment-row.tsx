"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Delete02Icon,
  ArrowTurnBackwardIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { NestedFieldMetadata } from "@/components/shared/form/form-context";
import { InputField } from "@/components/shared/form/input-field";
import { NumberField } from "@/components/shared/form/number-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { KOST_ZUWEISUNG_METADATA } from "@/lib/metadata/kost-zuweisung.generated";
import { cn } from "@/lib/utils";
import { CostAssignmentShare } from "./cost-assignment-share";
import { Kost2Warning } from "./kost2-warning";

export interface CostAssignmentRowProps {
  /**
   * Prefix of every field name of this row — the **full** path,
   * `positionen[1].kostZuweisungen[0].`, since [NestedFieldMetadata] replaces the enclosing position's
   * context rather than extending it.
   */
  prefix: string;
  /** 0-based index the row is stored with; shown so a row can be named in a confirmation. */
  index: number;
  deleted?: boolean;
  /** The row's own amount and the position's net sum, for the share it carries of it. */
  netto?: number | null;
  positionNetSum?: number | null;
  /** The chosen cost 2 unit, checked against the invoice's project; see [Kost2Warning]. */
  kost2Id?: number | null;
  onRemove?: () => void;
  onRestore?: () => void;
}

/**
 * One cost assignment of a position: which cost 1 and cost 2 unit how much of the position's net sum
 * goes to, and why.
 *
 * A compact line rather than a collapsible ([RepeatableRow]): the four fields *are* the row, so there
 * would be nothing left to fold away — and unlike Wicket's dialog, splitting a position across cost
 * units is done while the position is open, where the net sum it has to add up to is in view.
 */
export function CostAssignmentRow({
  prefix,
  index,
  deleted,
  netto,
  positionNetSum,
  kost2Id,
  onRemove,
  onRestore,
}: CostAssignmentRowProps) {
  const t = useTranslations();
  const label = useFieldLabels(KOST_ZUWEISUNG_METADATA);
  const format = useFormatContext();
  const name = (field: string) => `${prefix}${field}`;
  const rowLabel = `${t("fibu.rechnung.showKostZuweisungen")} ${index + 1}`;

  if (deleted) {
    return (
      <div className="flex items-center gap-2 rounded-md border border-dashed border-border bg-muted/40 px-3 py-2 text-sm text-muted-foreground line-through">
        <span className="min-w-0 flex-1">{rowLabel}</span>
        {onRestore && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="size-7 shrink-0"
            aria-label={`${t("undelete")}: ${rowLabel}`}
            onClick={onRestore}
          >
            <HugeiconsIcon icon={ArrowTurnBackwardIcon} size={14} />
          </Button>
        )}
      </div>
    );
  }

  return (
    <NestedFieldMetadata metadata={KOST_ZUWEISUNG_METADATA} namePrefix={prefix}>
      <div
        className={cn(
          "flex flex-wrap items-start gap-x-4 gap-y-3 rounded-md border border-border/60 bg-muted/30 px-3 py-2",
          "md:flex-nowrap"
        )}
      >
        <EntityAutocompleteField
          name={name("kost1")}
          label={label("kost1")}
          entity="cost1"
          className="min-w-0 flex-1 basis-40"
        />
        <EntityAutocompleteField
          name={name("kost2")}
          label={label("kost2")}
          entity="cost2"
          className="min-w-0 flex-1 basis-40"
        />
        {/* Beside the field it is about, as Wicket outlines that very field. */}
        <Kost2Warning kost2Id={kost2Id} />
        <NumberField
          name={name("netto")}
          label={label("netto")}
          // DECIMAL, not AMOUNT — `KostZuweisungDO.netto` is a plain `BigDecimal`, so the currency and
          // the two digits are passed rather than derived (as on an invoice position).
          fractionDigits={2}
          suffix={format.currency}
          align="right"
          className="min-w-0 flex-1 basis-32"
        />
        {/* Behind the amount, as in Wicket's table: kost 1, kost 2, amount, then the share it is. */}
        <CostAssignmentShare
          netto={netto}
          positionNetSum={positionNetSum}
          className="shrink-0 basis-16"
        />
        <InputField
          name={name("comment")}
          label={label("comment")}
          className="min-w-0 flex-1 basis-40"
        />
        {onRemove && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            // Aligned with the boxes, not with the labels above them.
            className="mt-6 size-8 shrink-0 text-muted-foreground hover:text-destructive"
            aria-label={`${t("delete")}: ${rowLabel}`}
            onClick={onRemove}
          >
            <HugeiconsIcon icon={Delete02Icon} size={14} />
          </Button>
        )}
      </div>
    </NestedFieldMetadata>
  );
}
