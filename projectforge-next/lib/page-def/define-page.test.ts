import { describe, expect, it } from "vitest";
import type { EntityMetadata, UIDataTypeName } from "@/lib/metadata/types";
import {
  alignFor,
  columnHeaderKeyOf,
  columnIdOf,
  defaultPinningOf,
  filterKindFor,
  labelKeyFor,
} from "./define-page";

const METADATA: EntityMetadata = {
  entity: "kost1",
  historizable: true,
  fields: {
    // Both a text of its own and the parent of fibu.kost1.title — the generator exports the leaf
    // as "fibu.kost1._".
    formattedNumber: {
      dataType: "STRING",
      i18nKey: "fibu.kost1",
      required: false,
    },
    description: {
      dataType: "STRING",
      i18nKey: "description",
      required: false,
    },
    bereich: { dataType: "INT", i18nKey: "fibu.kost1.bereich", required: true },
    // A field whose entity declares no @PropertyInfo.
    internal: { dataType: "STRING", required: false },
  },
};

/** The catalogue of the test: what the generator would have written for METADATA. */
const MESSAGES = new Set([
  "fibu.kost1._",
  "fibu.kost1.title.list",
  "fibu.kost1.bereich",
  "description",
]);
const hasMessage = (key: string) => MESSAGES.has(key);

describe("filterKindFor", () => {
  it("offers the number filter for everything counted", () => {
    for (const type of ["INT", "LONG", "DECIMAL", "AMOUNT"] as const) {
      expect(filterKindFor(type)).toBe("number");
    }
  });

  it("offers the date filter for every point in time", () => {
    for (const type of ["DATE", "TIMESTAMP", "TIME"] as const) {
      expect(filterKindFor(type)).toBe("date");
    }
  });

  it("offers no filter where there is nothing to compare", () => {
    expect(filterKindFor("BOOLEAN")).toBeNull();
    expect(filterKindFor("PICTURE")).toBeNull();
  });

  it("treats an enum as text — its data type is STRING and its values list is the filter", () => {
    expect(filterKindFor("STRING")).toBe("text");
  });

  it("has an answer for every data type the generator can emit", () => {
    const ALL: UIDataTypeName[] = [
      "AMOUNT",
      "BOOLEAN",
      "COST1",
      "COST2",
      "CUSTOMIZED",
      "DATE",
      "DECIMAL",
      "EMPLOYEE",
      "GROUP",
      "INT",
      "LONG",
      "KONTO",
      "LOCALE",
      "PASSWORD",
      "PICTURE",
      "STRING",
      "TASK",
      "TIME",
      "TIMESTAMP",
      "TIMEZONE",
      "USER",
    ];
    for (const type of ALL) {
      const kind = filterKindFor(type);
      expect(kind === null || ["text", "number", "date"].includes(kind)).toBe(
        true
      );
    }
  });
});

describe("alignFor", () => {
  it("right-aligns every kind of number", () => {
    expect(alignFor("DECIMAL")).toBe("right");
    expect(alignFor("AMOUNT")).toBe("right");
    expect(alignFor("INT")).toBe("right");
    expect(alignFor("LONG")).toBe("right");
  });

  it("leaves everything else alone — a date or a text is no quantity", () => {
    expect(alignFor("STRING")).toBe("left");
    expect(alignFor("DATE")).toBe("left");
    expect(alignFor("BOOLEAN")).toBe("left");
  });
});

describe("labelKeyFor", () => {
  it("takes the key the entity declares", () => {
    expect(labelKeyFor(METADATA, "description", hasMessage)).toBe(
      "description"
    );
    expect(labelKeyFor(METADATA, "bereich", hasMessage)).toBe(
      "fibu.kost1.bereich"
    );
  });

  it("resolves a key that is a leaf and a namespace at once to its exported leaf", () => {
    expect(labelKeyFor(METADATA, "formattedNumber", hasMessage)).toBe(
      "fibu.kost1._"
    );
  });

  it("falls back to the field name where the entity declares no key", () => {
    expect(labelKeyFor(METADATA, "internal", hasMessage)).toBe("internal");
  });

  it("falls back to the name of a field the metadata does not know at all", () => {
    expect(labelKeyFor(METADATA, "gone", hasMessage)).toBe("gone");
  });

  it("lets the declaration override the entity's wording", () => {
    expect(
      labelKeyFor(METADATA, "description", hasMessage, "attachments.short")
    ).toBe("attachments.short");
  });

  it("resolves the collision of an overriding key too", () => {
    expect(labelKeyFor(METADATA, "description", hasMessage, "fibu.kost1")).toBe(
      "fibu.kost1._"
    );
  });
});

describe("columnIdOf", () => {
  it("takes a field column's name, a computed column's id, and a period's begin", () => {
    expect(columnIdOf({ name: "description" })).toBe("description");
    expect(
      columnIdOf({
        id: "kunde.displayName",
        labelKey: "fibu.kunde._",
        accessor: () => "",
      })
    ).toBe("kunde.displayName");
    expect(
      columnIdOf({
        periodLabelKey: "fibu.periodOfPerformance",
        begin: "bereich",
        end: "internal",
      })
    ).toBe("bereich");
  });
});

describe("columnHeaderKeyOf", () => {
  it("takes the entity's key for a field column", () => {
    expect(columnHeaderKeyOf({ name: "bereich" }, METADATA)).toBe(
      "fibu.kost1.bereich"
    );
  });

  it("prefers the short header label over every other key", () => {
    expect(
      columnHeaderKeyOf(
        {
          name: "bereich",
          labelKey: "attachments._",
          headerLabelKey: "attachments.short",
        },
        METADATA
      )
    ).toBe("attachments.short");
  });

  it("takes a period's own label", () => {
    expect(
      columnHeaderKeyOf(
        {
          periodLabelKey: "fibu.periodOfPerformance",
          begin: "bereich",
          end: "internal",
        },
        METADATA
      )
    ).toBe("fibu.periodOfPerformance");
  });

  it("falls back to the column id where nothing names a key", () => {
    expect(columnHeaderKeyOf({ name: "internal" }, METADATA)).toBe("internal");
  });
});

describe("defaultPinningOf", () => {
  it("collects the pinned columns per edge, in declaration order", () => {
    expect(
      defaultPinningOf([
        { name: "bereich", pinned: "left" },
        { name: "description" },
        {
          periodLabelKey: "fibu.periodOfPerformance",
          begin: "internal",
          end: "description",
          pinned: "left",
        },
        {
          id: "actions",
          labelKey: "actions",
          accessor: () => "",
          pinned: "right",
        },
      ])
    ).toEqual({ left: ["bereich", "internal"], right: ["actions"] });
  });

  it("leaves out an edge nothing is pinned to, so nothing is stored as a change", () => {
    expect(defaultPinningOf([{ name: "description" }])).toEqual({});
  });
});
