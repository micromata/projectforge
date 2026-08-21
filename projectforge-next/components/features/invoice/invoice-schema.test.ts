import { describe, expect, it } from "vitest";
import { invoiceSchema } from "./invoice-schema";
import { emptyInvoiceValues } from "./invoice-values";

/**
 * The rules of the invoice form that are not simply the entity's — where the schema deliberately says less
 * than a reader might expect it to.
 */
describe("invoiceSchema", () => {
  it("accepts a payment target of negative days, which is read off the dates and not typed", () => {
    // A discount date before the invoice date is in the data (an invoice written after the fact), and both
    // day counts are derived from the dates for a stored invoice (see PaymentTermsFields). A `min: 0` here
    // therefore reported an error at a box nobody can edit and blocked every save and every e-invoice
    // export of such an invoice.
    const result = invoiceSchema.safeParse({
      ...emptyInvoiceValues(),
      datum: "2023-05-05",
      discountMaturity: "2023-05-03",
      discountZahlungsZielInTagen: -2,
      zahlungsZielInTagen: -2,
    });
    expect(result.success).toBe(true);
  });

  it("still refuses a day count that is not whole — a term is counted in days", () => {
    const result = invoiceSchema.safeParse({
      ...emptyInvoiceValues(),
      zahlungsZielInTagen: 1.5,
    });
    expect(result.success).toBe(false);
  });
});
