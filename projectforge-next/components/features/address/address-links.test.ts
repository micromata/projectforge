import { describe, expect, it } from "vitest";
import { websiteHref } from "./address-links";
import { phoneNumbersOf } from "./address-phones";

describe("websiteHref", () => {
  it("links http(s) as it is and gives everything else a scheme", () => {
    expect(websiteHref("https://example.org")).toBe("https://example.org");
    expect(websiteHref(" www.example.org ")).toBe("https://www.example.org");
    // Free text must not become a script url.
    expect(websiteHref("javascript:alert(1)")).toBe(
      "https://javascript:alert(1)"
    );
  });
});

describe("phoneNumbersOf", () => {
  it("lists the numbers to call in the order of the view, without the fax", () => {
    const numbers = phoneNumbersOf({
      id: 1,
      businessPhone: " 0561 1 ",
      fax: "0561 2",
      privateMobilePhone: "0171 3",
      mobilePhone: "",
    });
    expect(numbers.map((n) => [n.type, n.number, n.mobile])).toEqual([
      ["BUSINESS", "0561 1", false],
      ["PRIVATE_MOBILE", "0171 3", true],
    ]);
  });
});
