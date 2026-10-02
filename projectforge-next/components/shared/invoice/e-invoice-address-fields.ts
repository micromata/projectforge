import type { ComponentType } from "react";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { FieldNameOf, SectionDef } from "@/lib/page-def/types";

/** The properties an e-invoice is addressed with, by what they mean — each entity names them its own way. */
export interface EInvoiceAddressNames<M extends EntityMetadata> {
  contactPerson: FieldNameOf<M>;
  street: FieldNameOf<M>;
  zipCode: FieldNameOf<M>;
  city: FieldNameOf<M>;
  country: FieldNameOf<M>;
  vatId: FieldNameOf<M>;
  leitwegId: FieldNameOf<M>;
  eInvoiceEmail: FieldNameOf<M>;
}

/**
 * The fields of an "E-Rechnung" section, laid out the same wherever the address block appears — on an
 * account, where it is kept, and on an outgoing invoice, which is prefilled from it (see
 * `OutgoingInvoiceEntityRest.fillEInvoiceFieldsFromAccount`).
 *
 * The postal address on the left, as it reads on an envelope: contact person, street, then zip code,
 * city and country in one line (a four-column grid, so the zip code and the country stay narrow and the
 * city takes the width). On the right, as an aside that stays together however narrow the page gets,
 * what routes the e-invoice: the seller's bank account on top, then VAT id, Leitweg-ID and e-invoice
 * e-mail.
 *
 * A builder of declarations rather than a component, for two reasons: the properties are named
 * differently on each entity (`zipCode` vs. `customerZipCode`), and declared fields keep their labels,
 * rules and hints from each entity's own metadata. The bank account comes in as a component, because the
 * two selects store different things — an account the bank account's name, an invoice its IBAN.
 */
export function eInvoiceAddressFields<M extends EntityMetadata>({
  names,
  sellerBankAccount,
  streetRows,
}: {
  names: EInvoiceAddressNames<M>;
  sellerBankAccount: ComponentType<{ className?: string }>;
  /** Lines of the street box — an invoice's address is a free text of several lines. */
  streetRows?: number;
}): Pick<SectionDef<M>, "fields" | "aside" | "mainColumns"> {
  return {
    mainColumns: 4,
    fields: [
      { name: names.contactPerson, span: 4 },
      { name: names.street, span: 4, rows: streetRows },
      { name: names.zipCode },
      { name: names.city, span: 2 },
      { name: names.country },
    ],
    aside: [
      // First: which account the payment goes to is what the reader of this section looks for.
      { custom: sellerBankAccount },
      { name: names.vatId },
      { name: names.leitwegId, hintKey: "fibu.konto.leitwegId.tooltip" },
      { name: names.eInvoiceEmail },
    ],
  };
}
