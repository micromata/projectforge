import { describe, expect, it } from "vitest";
import { btNumberOf } from "./e-invoice-bt-numbers";

describe("btNumberOf", () => {
  it("names the term of a property that is sent", () => {
    expect(btNumberOf("RechnungDO", "customerOrderNumber")).toBe("BT-13");
    expect(btNumberOf("RechnungDO", "customerCostCenter")).toBe("BT-19");
    expect(btNumberOf("RechnungDO", "customerContactDepartment")).toBe("BT-56");
  });

  it("names the same terms on the account the invoice is prefilled from", () => {
    expect(btNumberOf("KontoDO", "costCenter")).toBe("BT-19");
    expect(btNumberOf("KontoDO", "contactDepartment")).toBe("BT-56");
  });

  it("says nothing about a property that is no part of the e-invoice", () => {
    expect(btNumberOf("RechnungDO", "bemerkung")).toBeUndefined();
    expect(btNumberOf("BookDO", "datum")).toBeUndefined();
    expect(btNumberOf(undefined, "datum")).toBeUndefined();
  });
});
