import { describe, expect, it } from "vitest";
import { hrefWithParams } from "@/lib/url-param";
import { addressDialogParams, parseAddressDialog } from "./address-dialog-url";

describe("address dialog url", () => {
  it("round-trips a dialog through the query", () => {
    const dialog = {
      kind: "call",
      addressId: 7,
      from: "addressView",
      number: "+49 561 316 793",
    } as const;
    const href = hrefWithParams(
      "/address",
      new URLSearchParams("q=ab"),
      addressDialogParams(dialog)
    );
    expect(href).toContain("q=ab");
    expect(parseAddressDialog(new URLSearchParams(href.split("?")[1]))).toEqual(
      dialog
    );
  });

  it("tells where the dialog was opened from", () => {
    const query = new URLSearchParams(
      addressDialogParams({
        kind: "sms",
        addressId: 3,
        from: "addressList",
        phoneType: "mobile",
      })
    );
    expect(parseAddressDialog(query)?.from).toBe("addressList");
  });

  it("ignores incomplete or invalid dialogs", () => {
    const base = "dialogFrom=addressList";
    expect(
      parseAddressDialog(new URLSearchParams(`${base}&dialog=sms`))
    ).toBeNull();
    expect(
      parseAddressDialog(
        new URLSearchParams(`${base}&dialog=sms&dialogAddress=x&phoneType=a`)
      )
    ).toBeNull();
    expect(
      parseAddressDialog(
        new URLSearchParams(`${base}&dialog=call&dialogAddress=3`)
      )
    ).toBeNull();
    // Without the place it was opened from, no instance would show it.
    expect(
      parseAddressDialog(
        new URLSearchParams("dialog=sms&dialogAddress=3&phoneType=a")
      )
    ).toBeNull();
  });
});
