"use client";

import { useCallback } from "react";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import {
  E_INVOICE_ACCOUNT_FIELDS,
  fetchEInvoiceFromAccount,
} from "@/lib/rs/invoice";

/**
 * Fills the e-invoice fields the form leaves empty from the invoice's account (else its customer's) — what
 * picking a customer or an account does, and what the "fill from account" button of the e-invoice section
 * does for an invoice that already names them.
 *
 * Only ever the **empty** fields, and checked twice: the backend fills nothing that was posted, and a field
 * the user typed into while the request was out is left alone as well — overwriting it would quietly undo
 * the entry.
 *
 * @returns How many fields were filled, for the button to say so; 0 where the form names no account.
 */
export function useFillEInvoiceFromAccount(): () => Promise<number> {
  const form = useEntityEditForm();
  return useCallback(async () => {
    const customer = form.getFieldValue("customer") as { id?: number } | null;
    const konto = form.getFieldValue("konto") as { id?: number } | null;
    if (customer?.id == null && konto?.id == null) return 0;
    const input = Object.fromEntries(
      E_INVOICE_ACCOUNT_FIELDS.map((name) => [name, form.getFieldValue(name)])
    );
    const filled = await fetchEInvoiceFromAccount({
      ...input,
      customer: customer?.id != null ? { id: customer.id } : null,
      konto: konto?.id != null ? { id: konto.id } : null,
    });
    let count = 0;
    for (const name of E_INVOICE_ACCOUNT_FIELDS) {
      const value = filled[name];
      if (value && !form.getFieldValue(name)) {
        form.setFieldValue(name, value);
        count++;
      }
    }
    return count;
  }, [form]);
}
