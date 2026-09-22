"use client";

import { useTranslations } from "next-intl";
import { NestedFieldMetadata } from "@/components/shared/form/form-context";
import { RepeatableRow } from "@/components/shared/form/repeatable-row";
import { EINGANGSRECHNUNGS_POSITION_METADATA } from "@/lib/metadata/eingangsrechnungs-position.generated";
import { CostAssignmentsSection } from "@/components/shared/invoice/cost-assignments-section";
import { PositionFields } from "./position-fields";
import { PositionRowHeader } from "./position-row-header";
import type { CreditorInvoicePositionValues } from "../creditor-invoice-schema";
import type { InvoicePositionSums } from "@/lib/rs/invoice-sums";

export interface PositionRowProps {
  position: CreditorInvoicePositionValues;
  /** Index in the form's `positionen` array — the row's field names are built from it. */
  index: number;
  /** Prefix of every field name of this row, e.g. `positionen[2].`. */
  prefix: string;
  sums: InvoicePositionSums | undefined;
  /** Absent where the invoice may not be written. */
  onRemove?: () => void;
  onRestore?: () => void;
  /**
   * Whether cost accounting is configured at all (`Configuration.isCostConfigured`, carried by the DTO as
   * `costConfigured`). False hides the cost assignments entirely, as the Wicket form does.
   */
  costConfigured: boolean;
}

/**
 * One incoming invoice position: what is billed, how much of it at what unit price and VAT rate, and how
 * its net sum is split across cost units.
 *
 * The collapsing row itself, composing [PositionFields] and the shared [CostAssignmentsSection]. Unlike
 * the outgoing invoice it hands the section neither a `defaultKost2` nor a `renderWarning`: a creditor
 * invoice has no project to preselect a cost unit from or to check one against.
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
      metadata={EINGANGSRECHNUNGS_POSITION_METADATA}
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
        <PositionFields prefix={prefix} sums={sums} />
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
