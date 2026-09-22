import { describe, expect, it } from "vitest";
import type { EntityMetadata } from "@/lib/metadata/types";
import { fromMetadata } from "./from-metadata";
import {
  INTEGER,
  REQUIRED,
  maxLengthMarker,
  maxMarker,
  minMarker,
} from "./markers";

/**
 * A hand-written stand-in for a generated file, so these tests state their own premises: a mandatory
 * string with a limit, an optional one with a different limit, one without any limit at all, and both
 * flavours of enum. Asserting against `book.generated.ts` instead would make a changed column length
 * in BookDO look like a bug in this module.
 */
const METADATA = {
  entity: "TestDO",
  historizable: false,
  fields: {
    title: { dataType: "STRING", required: true, maxLength: 255 },
    authors: { dataType: "STRING", required: false, maxLength: 1000 },
    // No maxLength: a non-string column, e.g. the "yyyy-MM-dd" of a LocalDate.
    lendOutDate: { dataType: "DATE", required: false },
    // The two flavours of number: one the entity insists on, one it doesn't.
    bereich: { dataType: "INT", required: true },
    endziffer: { dataType: "INT", required: false },
    status: {
      dataType: "STRING",
      required: true,
      enumValues: [{ value: "PRESENT" }, { value: "MISSED" }],
    },
    type: {
      dataType: "STRING",
      required: false,
      enumValues: [{ value: "BOOK", i18nKey: "book.type.book" }],
    },
  },
} as const satisfies EntityMetadata;

const m = fromMetadata(METADATA);

/** The marker of the first issue, or null if the value was accepted. */
function issue(schema: { safeParse: (v: unknown) => unknown }, value: unknown) {
  const result = schema.safeParse(value) as {
    success: boolean;
    error?: { issues: { message: string }[] };
  };
  return result.success ? null : (result.error?.issues[0]?.message ?? "");
}

describe("requiredString", () => {
  const schema = m.requiredString("title");

  it("reports a missing value as the REQUIRED marker, not as a Zod default", () => {
    expect(issue(schema, "")).toBe(REQUIRED);
    expect(issue(schema, "   ")).toBe(REQUIRED);
  });

  it("accepts the column's exact length and refuses one character more", () => {
    expect(issue(schema, "x".repeat(255))).toBeNull();
    expect(issue(schema, "x".repeat(256))).toBe(maxLengthMarker(255));
  });
});

describe("nullableString", () => {
  const schema = m.nullableString("authors");

  it("normalises blank to null, so the backend stores 'no value'", () => {
    expect(schema.parse("")).toBeNull();
    expect(schema.parse("   ")).toBeNull();
    expect(schema.parse(null)).toBeNull();
    expect(schema.parse("Kai")).toBe("Kai");
  });

  it("uses the limit of this property, not the one of another", () => {
    expect(issue(schema, "x".repeat(1000))).toBeNull();
    expect(issue(schema, "x".repeat(1001))).toBe(maxLengthMarker(1000));
  });

  it("accepts any length where the metadata declare none", () => {
    expect(issue(m.nullableString("lendOutDate"), "x".repeat(5000))).toBeNull();
  });
});

describe("intField", () => {
  const bounded = m.intField("bereich", { min: 0, max: 999 });

  it("reports an emptied box as missing rather than saving a 0", () => {
    expect(issue(bounded, null)).toBe(REQUIRED);
    expect(issue(bounded, 0)).toBeNull();
  });

  it("keeps a value out of range from ever being sent", () => {
    expect(issue(bounded, 1000)).toBe(maxMarker(999));
    expect(issue(m.intField("bereich", { min: 1 }), 0)).toBe(minMarker(1));
    expect(issue(bounded, 999)).toBeNull();
  });

  it("refuses a fraction, which no number segment can hold", () => {
    expect(issue(bounded, 1.5)).toBe(INTEGER);
  });

  it("leaves an optional field empty without complaining", () => {
    expect(
      issue(m.intField("endziffer", { min: 0, max: 99 }), null)
    ).toBeNull();
  });

  it("needs no bounds at all — they are the caller's, not the metadata's", () => {
    expect(issue(m.intField("endziffer"), 12345)).toBeNull();
  });
});

describe("enumField", () => {
  it("refuses a constant the backend enum does not have", () => {
    expect(issue(m.enumField("status"), "UNKNOWN")).not.toBeNull();
    expect(issue(m.enumField("status"), "PRESENT")).toBeNull();
  });

  it("reports null on a mandatory enum and accepts it on an optional one", () => {
    expect(issue(m.enumField("status"), null)).toBe(REQUIRED);
    expect(issue(m.enumField("type"), null)).toBeNull();
  });
});

describe("enumOptions", () => {
  it("labels a constant with its i18nKey, in the order the enum declares them", () => {
    expect(m.enumOptions("type", (key) => `t:${key}`)).toEqual([
      { value: "BOOK", label: "t:book.type.book" },
    ]);
  });

  it("falls back to the constant name where the enum has no i18nKey", () => {
    expect(m.enumOptions("status", (key) => `t:${key}`)).toEqual([
      { value: "PRESENT", label: "PRESENT" },
      { value: "MISSED", label: "MISSED" },
    ]);
  });
});

describe("field", () => {
  it("throws on a name the metadata do not know instead of assuming it is optional", () => {
    // Cast: the point is the runtime guard, which tsc would not let a call site reach.
    expect(() =>
      m.nullableString("renamedInTheEntity" as "authors")
    ).toThrowError(/no field "renamedInTheEntity"/);
  });

  it("throws when a non-enum property is used as one", () => {
    expect(() => m.enumOptions("title" as "type", (k) => k)).toThrowError(
      /no enum property/
    );
  });
});
