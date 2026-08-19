import { describe, expect, it } from "vitest";
import type { EntityMetadata, UIDataTypeName } from "@/lib/metadata/types";
import {
  alignFor,
  columnHeaderKeyOf,
  columnIdOf,
  defaultPinningOf,
  defineListPage,
  filterKindFor,
  labelKeyFor,
  visibleColumnsOf,
} from "./define-page";
import type { ColumnDeclaration } from "./types";
import type { ListRow } from "@/hooks/use-entity-list-page";

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

describe("visibleColumnsOf", () => {
  const COLUMNS: ColumnDeclaration<ListRow, typeof METADATA>[] = [
    { name: "description" },
    { name: "bereich", visible: ({ variables }) => variables?.orders === true },
    { name: "internal", visible: () => true },
  ];

  it("keeps a column without a predicate and one whose predicate holds", () => {
    expect(visibleColumnsOf(COLUMNS, { orders: true }).map(columnIdOf)).toEqual(
      ["description", "bereich", "internal"]
    );
  });

  it("drops the column the backend says this user does not have", () => {
    expect(
      visibleColumnsOf(COLUMNS, { orders: false }).map(columnIdOf)
    ).toEqual(["description", "internal"]);
  });

  it("drops it while the meta data has not arrived yet, rather than flashing the column", () => {
    expect(visibleColumnsOf(COLUMNS, undefined).map(columnIdOf)).toEqual([
      "description",
      "internal",
    ]);
  });

  it("answers the very same array where nothing was dropped, so the columns keep their identity", () => {
    const plain: ColumnDeclaration<ListRow, typeof METADATA>[] = [
      { name: "description" },
    ];
    expect(visibleColumnsOf(plain, undefined)).toBe(plain);
  });

  it("keeps a dropped column out of the default pinning as well", () => {
    const columns: ColumnDeclaration<ListRow, typeof METADATA>[] = [
      { name: "description", pinned: "left" },
      { name: "bereich", pinned: "left", visible: () => false },
    ];
    expect(defaultPinningOf(visibleColumnsOf(columns, undefined))).toEqual({
      left: ["description"],
    });
  });
});

describe("defineListPage", () => {
  interface Row extends ListRow {
    formattedNumber?: string;
  }

  /** A list whose entries are still edited in the legacy page — see PageDef.edit. */
  const LIST_ONLY = defineListPage<Row, typeof METADATA>({
    entity: "cost1",
    metadata: METADATA,
    route: "/cost1",
    queryKey: ["cost1"],
    categoryKey: "menu.fibu._",
    titleKey: "fibu.kost1.title.list",
    columns: [{ name: "formattedNumber" }],
    massUpdate: { endpoint: "cost1Selected", route: "/cost1/mass-update" },
  });

  it("declares a complete list without a form", () => {
    expect(LIST_ONLY.edit).toBeUndefined();
    // What the list renderer reads is all there, so nothing had to be invented to satisfy a type.
    expect(columnIdOf(LIST_ONLY.columns[0])).toBe("formattedNumber");
    expect(LIST_ONLY.massUpdate?.endpoint).toBe("cost1Selected");
  });
});
