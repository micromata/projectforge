"use client";

import { useTranslations } from "next-intl";
import { NestedFieldMetadata } from "@/components/shared/form/form-context";
import { RepeatableRow } from "@/components/shared/form/repeatable-row";
import { RECHNUNGS_POSITION_METADATA } from "@/lib/metadata/rechnungs-position.generated";
import { CostAssignmentsSection } from "./cost-assignments-section";
import { PositionFields } from "./position-fields";
import { PositionRowHeader } from "./position-row-header";
import type { InvoicePositionValues } from "../invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice";

export interface PositionRowProps {
  position: InvoicePositionValues;
  /** Index in the form's `positionen` array — the row's field names are built from it. */
  index: number;
  /** Prefix of every field name of this row, e.g. `positionen[2].`. */
  prefix: string;
  sums: InvoicePositionSums | undefined;
  /** Absent where the invoice may not be written. */
  onRemove?: () => void;
  onRestore?: () => void;
  /**
   * Whether cost accounting is configured at all (`Configuration.isCostConfigured`, carried by the DTO
   * as `costConfigured`). False hides the cost assignments entirely, as Wicket's form does.
   */
  costConfigured: boolean;
}

/**
 * One invoice position: what is billed, how much of it at what unit price and VAT rate, the period it
 * covers, and how its net sum is split across cost units.
 *
 * The collapsing row itself, composing [PositionFields] and [CostAssignmentsSection]. Hand-written JSX
 * rather than a declaration, for the reason the order's [PositionRow] is: what a position looks like — a
 * period that appears only when it has its own, a nested table of cost assignments — is this entity's
 * business. Every *mechanism* is the shared one: the collapsing row, the fields, and their labels and
 * rules from the position's own metadata ([NestedFieldMetadata]).
 */
export function PositionRow({
  position,
  index,
  prefix,
  sums,
  onRemove,
  onRestore,
  costConfigured,
}: PositionRowProps) {
  const t = useTranslations();
  const writeAccess = !!onRemove;

  return (
    <NestedFieldMetadata
      metadata={RECHNUNGS_POSITION_METADATA}
      namePrefix={prefix}
    >
      <RepeatableRow
        header={
          <PositionRowHeader
            position={position}
            sums={sums}
            costConfigured={costConfigured}
          />
        }
        // A row just added is there to be filled in; a stored one stays folded, which is what makes an
        // invoice of a dozen positions readable at all.
        defaultOpen={position.id == null}
        deleted={position.deleted}
        onRemove={onRemove}
        onRestore={onRestore}
        removeLabel={
          position.text ??
          `${t("label.position.short")} ${position.number ?? index + 1}`
        }
        // Marked as needing attention where the cost assignments don't add up — the red Wicket paints.
        highlighted={
          costConfigured &&
          sums?.kostZuweisungNetFehlbetrag != null &&
          sums.kostZuweisungNetFehlbetrag !== 0
        }
      >
        <PositionFields position={position} prefix={prefix} sums={sums} />
        {costConfigured && (
          <CostAssignmentsSection
            prefix={prefix}
            sums={sums}
            writeAccess={writeAccess}
            className="md:col-span-3"
          />
        )}
      </RepeatableRow>
    </NestedFieldMetadata>
  );
}
