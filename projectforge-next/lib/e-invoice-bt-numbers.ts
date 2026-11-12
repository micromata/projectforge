/**
 * The business terms (BT) of the EN 16931 e-invoice standard that the properties of an entity end up in,
 * by entity (`EntityMetadata.entity`) and property. Shown as a small hint behind the label, so the
 * sender and a customer's accounting can talk about the same field ("please fill BT-19").
 *
 * Only properties the export (`EInvoiceExportService`) really writes to the XML are listed — a hint
 * on a field that is not sent would promise something the document does not contain.
 */
const BT_NUMBERS: Record<string, Record<string, string>> = {
  RechnungDO: {
    nummer: "BT-1",
    datum: "BT-2",
    typ: "BT-3",
    currency: "BT-5",
    faelligkeit: "BT-9",
    customerLeitwegId: "BT-10",
    // Sent as BT-10 where there is no Leitweg-ID (see EInvoiceExportService.buyerReferenceOf).
    customerref1: "BT-10",
    customerOrderNumber: "BT-13",
    customerCostCenter: "BT-19",
    originalRechnung: "BT-25",
    periodOfPerformanceBegin: "BT-73",
    periodOfPerformanceEnd: "BT-74",
    customerVatId: "BT-48",
    customerEInvoiceEmail: "BT-49",
    customerContactEmail: "BT-58",
    sellerContactEmail: "BT-43",
    customerAddress: "BT-50",
    customerCity: "BT-52",
    customerZipCode: "BT-53",
    customerCountry: "BT-55",
    customerContactPerson: "BT-56",
    customerContactDepartment: "BT-56",
    sellerBankAccount: "BT-84",
  },
  // The e-invoice block of an account, which an invoice is prefilled from (the same terms).
  KontoDO: {
    leitwegId: "BT-10",
    costCenter: "BT-19",
    vatId: "BT-48",
    eInvoiceEmail: "BT-49",
    contactEmail: "BT-58",
    street: "BT-50",
    city: "BT-52",
    zipCode: "BT-53",
    country: "BT-55",
    contactPerson: "BT-56",
    contactDepartment: "BT-56",
    sellerBankAccountName: "BT-84",
  },
};

/** The BT number(s) of a property, e.g. `"BT-13"`; undefined where the property is no part of the e-invoice. */
export function btNumberOf(
  entity: string | undefined,
  property: string | undefined
): string | undefined {
  if (!entity || !property) return undefined;
  return BT_NUMBERS[entity]?.[property];
}
