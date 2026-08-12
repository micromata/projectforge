"use client";

import { useTranslations } from "next-intl";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { useFieldArray } from "@/hooks/use-field-array";
import { emptyPositionValues } from "../order-values";
import { useOrderSums } from "../use-order-sums";
import { PositionRow } from "./position-row";
import type { OrderPositionValues } from "../order-schema";
import type { OrderDetail } from "../types";

/**
 * The positions of an order: the collection that makes this page the hard case — any number of rows,
 * each a form of its own, each carrying sums the server computes.
 *
 * The mechanics are the shared ones ([useFieldArray], [RepeatableList]); what a row looks like is
 * [PositionRow]. What is decided here is only which rows may be removed and who is allowed to touch the
 * invoice flag.
 *
 * @param id null while the order is being added — then there is nothing invoiced and nothing to look up.
 */
export function PositionsSection({ id }: { id: number | null }) {
  const t = useTranslations();
  const array = useFieldArray<OrderPositionValues>("positionen");
  const { positionSums } = useOrderSums();
  // A cache read of the order the form was filled from, not a second request: the invoice sums and the
  // access flags are the server's and are not part of the form's values (see PositionInvoices).
  const order = useEntityDetail<OrderDetail>("order", id).data;
  const invoiceInfoById = new Map(
    (order?.positionen ?? [])
      .filter((pos) => pos.id != null)
      .map((pos) => [pos.id, pos])
  );
  // New orders are editable by definition — the flags only come with a loaded one.
  const writeAccess = id == null || order?.writeAccess === true;

  return (
    <RepeatableList
      array={array}
      emptyText={t("fibu.auftrag.error.auftragHatKeinePositionen")}
      addLabel={writeAccess ? t("fibu.auftrag.tooltip.addPosition") : undefined}
      onAdd={writeAccess ? () => array.add(emptyPositionValues()) : undefined}
      row={(position, index) => {
        const invoiceInfo =
          position.id == null ? undefined : invoiceInfoById.get(position.id);
        return (
          <PositionRow
            position={position}
            index={index}
            prefix={array.fieldName(index, "")}
            sums={positionSums(position.number)}
            // Removing a position an invoice points at would leave that invoice pointing at nothing —
            // Wicket hides the button for the same reason (`positionInInvoiceExists`).
            onRemove={
              writeAccess && !invoiceInfo?.invoicedElsewhere
                ? () => array.remove(index)
                : undefined
            }
            invoiceWriteAccess={
              order?.vollstaendigFakturiertWriteAccess === true
            }
            invoiceInfo={invoiceInfo}
          />
        );
      }}
    />
  );
}
