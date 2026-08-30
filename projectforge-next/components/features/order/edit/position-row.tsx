"use client";

import { useTranslations } from "next-intl";
import { AUFTRAGS_POSITION_METADATA } from "@/lib/metadata/auftrags-position.generated";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { DatePeriodField } from "@/components/shared/form/date-period-field";
import { TaskSelectField } from "@/components/shared/tasks/task-select-field";
import { JiraIssuesLinks } from "@/components/shared/jira/jira-issues-links";
import { InputField } from "@/components/shared/form/input-field";
import { NestedFieldMetadata } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { RepeatableRow } from "@/components/shared/form/repeatable-row";
import { SelectField } from "@/components/shared/form/select-field";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { TERM_KIND_IDS } from "@/lib/date-period";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { PositionInvoices } from "./position-invoices";
import { PositionRowHeader } from "./position-row-header";
import type { OrderPositionValues } from "../order-schema";
import type { PositionInvoiceInfo } from "../types";
import type { OrderPositionSums } from "@/lib/rs/order";

const p = fromMetadata(AUFTRAGS_POSITION_METADATA);

export interface PositionRowProps {
  position: OrderPositionValues;
  /** Index in the form's `positionen` array — the row's field names are built from it. */
  index: number;
  /** Prefix of every field name of this row, e.g. `positionen[2].`. */
  prefix: string;
  sums: OrderPositionSums | undefined;
  /** Absent when the position must not be removed — an invoice references it, or write access is missing. */
  onRemove?: () => void;
  /** Takes a soft-deleted position back — see [RepeatableRow], which renders the deleted state. */
  onRestore?: () => void;
  /**
   * Whether the `vollstaendigFakturiert` checkbox may be ticked — the invoice right and PF_Finance, which
   * is what `AuftragRight` enforces on write. False shows the flag read-only rather than hiding it, with
   * the very message the backend would answer a change with.
   */
  invoiceFlagWriteAccess: boolean;
  /**
   * Whether the invoice numbers may link to the invoice's own page — the select access on outgoing
   * invoices. False shows them as plain text, as Wicket's `InvoicePositionsPanel` does for a non-finance
   * reader (see `PositionInvoices`).
   */
  invoicesSelectAccess: boolean;
  /**
   * Read-only invoice data of this position, from the loaded order rather than from the form — absent
   * for a position that was never invoiced (and for one that was never saved).
   */
  invoiceInfo?: PositionInvoiceInfo;
}

/**
 * One order position: everything of `AuftragsPositionDO` a user edits, plus the read-only sums and
 * invoice links the backend fills in.
 *
 * Hand-written JSX rather than a declaration, and deliberately so (see `useFieldArray`): what a position
 * looks like — a period of performance that appears only when it has its own, a task, an invoice
 * summary — is this entity's business, and describing that declaratively would be a second form
 * framework. What the row does take from the shared layer is every mechanism: the collapsing row, the
 * fields, and their labels and rules from the position's own metadata ([NestedFieldMetadata]).
 */
export function PositionRow({
  position,
  index,
  prefix,
  sums,
  onRemove,
  onRestore,
  invoiceFlagWriteAccess,
  invoicesSelectAccess,
  invoiceInfo,
}: PositionRowProps) {
  const t = useTranslations();
  const label = useFieldLabels(AUFTRAGS_POSITION_METADATA);
  const format = useFormatContext();
  const name = (field: string) => `${prefix}${field}`;
  // The order's own period applies unless the position was given one — then, and only then, are the two
  // date fields and the mode of payment hers to fill in (`PeriodOfPerformanceType`, and Wicket's form
  // does the same).
  const ownPeriod = position.periodOfPerformanceType === "OWN";

  return (
    <NestedFieldMetadata
      metadata={AUFTRAGS_POSITION_METADATA}
      namePrefix={prefix}
    >
      <RepeatableRow
        header={
          <PositionRowHeader
            position={position}
            sums={sums}
            invoiceInfo={invoiceInfo}
            canOpenInvoice={invoicesSelectAccess}
          />
        }
        // A row just added is there to be filled in; a stored one stays folded, which is what makes an
        // order of a dozen positions readable at all.
        defaultOpen={position.id == null}
        deleted={position.deleted}
        onRemove={onRemove}
        onRestore={onRestore}
        removeLabel={
          position.titel ?? `${t("fibu.auftrag.position._")} ${index + 1}`
        }
        // Overdue, as Wicket's form marks it (`AuftragEditForm.refreshPositions`): the position or its
        // order is closed, or a reached payment schedule entry points at it — not merely "not fully
        // invoiced yet", which is true of every open commissioned position.
        highlighted={!!sums?.toBeInvoiced}
      >
        <InputField
          name={name("titel")}
          label={label("titel")}
          className="md:col-span-2"
        />
        <SelectField
          name={name("status")}
          label={label("status")}
          options={p.enumOptions("status", t)}
          emphasized
        />
        <NumberField
          name={name("nettoSumme")}
          label={label("nettoSumme")}
          // DECIMAL, not AMOUNT: `AuftragsPositionDO.nettoSumme` is a plain `BigDecimal`, so the
          // currency and the two digits are passed explicitly rather than derived from the data type.
          fractionDigits={2}
          suffix={format.currency}
        />
        <NumberField
          name={name("personDays")}
          label={label("personDays")}
          fractionDigits={2}
        />
        <SelectField
          name={name("art")}
          label={label("art")}
          options={p.enumOptions("art", t)}
        />
        <SelectField
          name={name("paymentType")}
          label={label("paymentType")}
          options={p.enumOptions("paymentType", t)}
        />
        <SelectField
          name={name("forecastType")}
          label={label("forecastType")}
          hint={t("fibu.auftrag.forecastType.pos.info")}
          options={p.enumOptions("forecastType", t)}
        />
        <TaskSelectField name={name("task")} label={label("task")} />
        <SelectField
          name={name("periodOfPerformanceType")}
          label={label("periodOfPerformanceType")}
          options={p.enumOptions("periodOfPerformanceType", t)}
        />
        {ownPeriod && (
          <>
            <DatePeriodField
              label={t("fibu.periodOfPerformance._")}
              begin={{
                name: name("periodOfPerformanceBegin"),
                label: label("periodOfPerformanceBegin"),
              }}
              end={{
                name: name("periodOfPerformanceEnd"),
                label: label("periodOfPerformanceEnd"),
              }}
              periodKinds={TERM_KIND_IDS}
              paging
            />
            <SelectField
              name={name("modeOfPaymentType")}
              label={label("modeOfPaymentType")}
              options={p.enumOptions("modeOfPaymentType", t)}
            />
          </>
        )}
        <TextAreaField
          name={name("bemerkung")}
          label={label("bemerkung")}
          rows={2}
          className="md:col-span-3"
        />
        {/* JIRA issue keys of the position's comment as links, below it — the position's free-text
            field, like the order's own note fields (see JiraIssuesLinks). Reads the live value from the
            reactive `position` prop. */}
        <JiraIssuesLinks text={position.bemerkung} className="md:col-span-3" />
        {/* Always rendered — the flag says something about the position everybody who may read the order
            should see; only ticking it is the accounting staff's. */}
        <CheckboxField
          name={name("vollstaendigFakturiert")}
          label={label("vollstaendigFakturiert")}
          disabled={!invoiceFlagWriteAccess}
          // Only where it explains why the box cannot be ticked — and then in the backend's own words,
          // the message `AuftragRight` would answer the save with.
          hint={
            invoiceFlagWriteAccess
              ? undefined
              : t("fibu.auftrag.error.vollstaendigFakturiertProtection")
          }
        />
        {invoiceInfo?.invoicedElsewhere && (
          <PositionInvoices
            invoiceInfo={invoiceInfo}
            canOpenInvoice={invoicesSelectAccess}
            className="md:col-span-2"
          />
        )}
      </RepeatableRow>
    </NestedFieldMetadata>
  );
}
