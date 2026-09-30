"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { RECHNUNG_METADATA } from "@/lib/metadata/rechnung.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { InvoiceSumsLine } from "@/components/shared/invoice/invoice-sums-line";
import { paidState } from "@/components/shared/invoice/paid-state";
import { StatusPill, type StatusTone } from "@/components/shared/status-pill";
import { InvoiceLink } from "./invoice-link";
import { ReferencedOrders } from "./referenced-orders";
import type { InvoiceValues } from "../invoice-schema";
import type { InvoiceDetail, InvoiceRef } from "../types";

const m = fromMetadata(RECHNUNG_METADATA);

/**
 * The traffic-light tone each invoice status reads as: paid is green, the reminders and the dunning red
 * (money is late). Planned and cancelled carry no urgency, so they stay neutral. A status without an entry
 * falls back to neutral rather than mis-colouring.
 *
 * GESTELLT is deliberately absent: an issued invoice is refined into its paid state (open / overdue / paid,
 * see below), so it never reaches this map.
 */
const STATUS_TONE: Record<string, StatusTone> = {
  BEZAHLT: "success",
  ZAHLUNGSERINNERUNG1: "danger",
  ZAHLUNGSERINNERUNG2: "danger",
  GEMAHNT: "danger",
  GEPLANT: "neutral",
  STORNIERT: "neutral",
};

/**
 * Sticky banner between the tab strip and the scrollable sections — stays in view while the user scrolls
 * through the positions.
 *
 * Shows the invoice number, its status and type badges and the live running sums, so the reader never has
 * to scroll back to the head section to check what they are editing.
 */
export function InvoiceEditBanner() {
  const t = useTranslations();
  const form = useEntityEditForm();

  // Subscribe only to the fields the banner shows so it doesn't re-render on every keystroke; the payment
  // dates feed the paid state an issued invoice is shown as.
  const { nummer, originalNummer, status, typ, bezahlDatum, faelligkeit } =
    useStore(
      form.store,
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (state: any) => {
        const v = state.values as InvoiceValues;
        return {
          nummer: v.nummer,
          originalNummer: v.originalInvoice?.nummer ?? null,
          status: v.status,
          typ: v.typ,
          bezahlDatum: v.bezahlDatum,
          faelligkeit: v.faelligkeit,
        };
      }
    );

  // An issued invoice reads as its payment sub-state (unpaid / overdue / paid), derived from the dates like
  // the list does; every other status keeps its enum text and mapped colour. A cancellation is settled by
  // the invoice it cancels, so it is never unpaid or overdue.
  const isIssued = status === "GESTELLT" && typ !== "CANCELLATION";
  const paid = isIssued ? paidState(bezahlDatum, faelligkeit) : null;
  const statusLabel = paid
    ? t(paid.labelKey)
    : m.enumOptions("status", t).find((o) => o.value === status)?.label;
  const statusTone = paid
    ? paid.tone
    : (STATUS_TONE[status ?? ""] ?? "neutral");
  const typLabel = m.enumOptions("typ", t).find((o) => o.value === typ)?.label;
  // A cancellation has no number of its own; it is known by the cancelled invoice's plus "-S"
  // (`RechnungDO.belegNummer`), derived here live so a number typed into the original's box shows at once.
  const belegNummer =
    typ === "CANCELLATION"
      ? originalNummer != null
        ? `${originalNummer}-S`
        : null
      : nummer;
  // Only on the loaded entry: whether a cancellation exists is the backend's to know.
  const cancelledBy = useEntityData<InvoiceDetail>()?.cancellationInvoice;

  return (
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-b bg-background px-6 py-2">
      <div className="flex shrink-0 items-center gap-2">
        {/* Absent on a planned invoice and on a credit note announced by the customer — `RechnungDao`
            assigns the number on the transition out of GEPLANT, and the latter never gets one. */}
        {belegNummer != null && (
          <span className="text-sm font-semibold tabular-nums">
            #{belegNummer}
          </span>
        )}
        {statusLabel && <StatusPill tone={statusTone} label={statusLabel} />}
        {typLabel && (
          <Badge variant="outline" className="font-normal">
            {typLabel}
          </Badge>
        )}
        {cancelledBy && <CancelledByLink cancellation={cancelledBy} />}
      </div>
      <ReferencedOrders className="shrink-0" />
      <InvoiceSumsLine
        entity="outgoingInvoice"
        className="ml-auto justify-end"
      />
    </div>
  );
}

/** "Cancelled by 16956-S", linking the cancellation — so an invoice that is void says so where it is read. */
function CancelledByLink({ cancellation }: { cancellation: InvoiceRef }) {
  const t = useTranslations();
  if (cancellation.id == null) return null;
  return (
    <InvoiceLink invoiceId={cancellation.id} className="text-sm">
      {t("fibu.rechnung.cancellation.cancelledBy", {
        arg0: cancellation.belegNummer ?? String(cancellation.id),
      })}
    </InvoiceLink>
  );
}
