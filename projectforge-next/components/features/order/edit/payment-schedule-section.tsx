"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { useFieldArray } from "@/hooks/use-field-array";
import { emptyScheduleValues } from "../order-values";
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

  // Only positions that have a number can be referred to: the number is assigned on save, and an
  // instalment pointing at nothing would be rejected by `AuftragDao`.
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
      onAdd={writeAccess ? () => array.add(emptyScheduleValues()) : undefined}
      row={(schedule, index) => (
        <PaymentScheduleRow
          schedule={schedule}
          index={index}
          prefix={array.fieldName(index, "")}
          positionOptions={positionOptions}
          onRemove={writeAccess ? () => array.remove(index) : undefined}
          invoiceWriteAccess={order?.vollstaendigFakturiertWriteAccess === true}
        />
      )}
    />
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: { positionen: OrderPositionValues[] };
}
