"use client";

import { useTranslations } from "next-intl";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { useFieldArray } from "@/hooks/use-field-array";
import { emptyPositionValues, nextPositionNumber } from "../invoice-values";
import { useInvoiceFormDefaults } from "../use-invoice-form-defaults";
import { useInvoiceSums } from "../use-invoice-sums";
import { PositionRow } from "./position-row";
import type { InvoicePositionValues } from "../invoice-schema";
import type { InvoiceDetail } from "../types";

/**
 * The positions of an invoice: the collection that makes this page the hard case — any number of rows,
 * each carrying a collection of its own ([CostAssignmentsSection]) and sums the server computes.
 *
 * The mechanics are the shared ones ([useFieldArray], [RepeatableList]); what a row looks like is
 * [PositionRow]. What is decided here is only who may change the rows and whether cost accounting is
 * configured at all.
 *
 * @param id null while the invoice is being added — then there are no access flags to look up.
 */
export function PositionsSection({ id }: { id: number | null }) {
  const t = useTranslations();
  const array = useFieldArray<InvoicePositionValues>("positionen");
  const { positionSums } = useInvoiceSums();
  // The configured `fibu.defaultVAT`, for the first position of an invoice — from then on the row above
  // is the better guess (see emptyPositionValues).
  const defaults = useInvoiceFormDefaults();
  // A cache read of the invoice the form was filled from, not a second request: the access flags and
  // whether cost ids are configured are the server's and are not part of the form's values.
  const invoice = useEntityDetail<InvoiceDetail>("outgoingInvoice", id).data;
  // New invoices are editable by definition — the flags only come with a loaded one.
  const writeAccess = id == null || invoice?.writeAccess === true;
  // Absent on a new invoice, where nothing has been loaded yet: assume configured rather than hiding the
  // cost assignments of every position the user is about to fill in. An installation without cost ids
  // answers false as soon as the first read comes back.
  const costConfigured = invoice?.costConfigured !== false;

  return (
    <RepeatableList
      array={array}
      emptyText={t("fibu.rechnung.error.rechnungHatKeinePositionen")}
      addLabel={
        writeAccess ? t("fibu.rechnung.tooltip.addPosition") : undefined
      }
      // A new position is numbered here rather than only on save, because its header shows the number
      // (see emptyPositionValues); the backend still has the last word.
      onAdd={
        writeAccess
          ? () =>
              array.add(
                emptyPositionValues(
                  nextPositionNumber(array.rows),
                  // The row it is added below, so its VAT rate carries over.
                  array.rows[array.rows.length - 1],
                  defaults?.defaultVat ?? null
                )
              )
          : undefined
      }
      row={(position, index) => (
        <PositionRow
          position={position}
          index={index}
          prefix={array.fieldName(index, "")}
          sums={positionSums(position.number)}
          onRemove={writeAccess ? () => array.remove(index) : undefined}
          onRestore={writeAccess ? () => array.restore(index) : undefined}
          costConfigured={costConfigured}
        />
      )}
    />
  );
}
