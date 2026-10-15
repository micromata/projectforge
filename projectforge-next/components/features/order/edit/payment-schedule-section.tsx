"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { useFieldArray } from "@/hooks/use-field-array";
import { emptyScheduleValues, nextScheduleNumber } from "../order-values";
import { PaymentScheduleRow } from "./payment-schedule-row";
import type {
  OrderPositionValues,
  PaymentScheduleValues,
} from "../order-schema";
import type { OrderDetail } from "../types";

/**
 * The payment schedule of an order: when which part of which position becomes due.
 *
 * The second nested collection of this page, and the reason the mechanics were made shared rather than
 * built into the positions ([useFieldArray]). Its rows refer to the positions of the *same form*, so the
 * choices are read from the form store and follow along while positions are added.
 */
export function PaymentScheduleSection({ id }: { id: number | null }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const array = useFieldArray<PaymentScheduleValues>("paymentSchedules");
  const order = useEntityDetail<OrderDetail>("order", id).data;
  const writeAccess = id == null || order?.writeAccess === true;

  // Only positions that have a number can be referred to — an instalment pointing at nothing would be
  // rejected by `AuftragDao`. A position added in this form already carries a provisional one, so it can
  // be picked before the order is saved (see emptyPositionValues); the backend renumbers both together.
  const positions = useStore(form.store, (state: unknown) =>
    ((state as FormState).values.positionen ?? []).filter(
      (pos) => !pos.deleted && pos.number != null
    )
  ) as OrderPositionValues[];
  const positionOptions = positions.map((pos) => ({
    value: String(pos.number),
    label: [`${t("label.position.short")} ${pos.number}`, pos.titel]
      .filter(Boolean)
      .join(": "),
  }));

  return (
    <RepeatableList
      array={array}
      emptyText={t("fibu.auftrag.paymentschedule._")}
      addLabel={
        writeAccess ? t("fibu.auftrag.tooltip.addPaymentschedule") : undefined
      }
      // Numbered here already, as a position is: the number the row's header shows has to be the one it
      // is stored with (see emptyScheduleValues).
      onAdd={
        writeAccess
          ? () => array.add(emptyScheduleValues(nextScheduleNumber(array.rows)))
          : undefined
      }
      row={(schedule, index) => (
        <PaymentScheduleRow
          schedule={schedule}
          prefix={array.fieldName(index, "")}
          positionOptions={positionOptions}
          onRemove={writeAccess ? () => array.remove(index) : undefined}
          // Restoring is gated by the write access alone: it undoes a deletion the same user made in
          // this form, and the row it brings back is the one that was already there.
          onRestore={writeAccess ? () => array.restore(index) : undefined}
          invoiceFlagWriteAccess={
            order?.vollstaendigFakturiertWriteAccess === true
          }
        />
      )}
    />
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: { positionen: OrderPositionValues[] };
}
