import { describe, expect, it } from "vitest";
import type { EntityMetadata } from "@/lib/metadata/types";
import { auditColumnsFor, defaultVisibilityOf } from "./audit-columns";
import type { ColumnDeclaration } from "./types";

const METADATA: EntityMetadata = {
  entity: "kost1",
  historizable: true,
  fields: {
    description: {
      dataType: "STRING",
      i18nKey: "description",
      required: false,
    },
    created: { dataType: "TIMESTAMP", i18nKey: "created", required: false },
    lastUpdate: { dataType: "TIMESTAMP", i18nKey: "modified", required: false },
  },
};

/** An entity the generator found no timestamps on — a DTO the backend fills itself. */
const WITHOUT_TIMESTAMPS: EntityMetadata = {
  entity: "transient",
  historizable: false,
  fields: METADATA.fields.description
    ? { description: METADATA.fields.description }
    : {},
};

type Row = { id: number };
const columns = (...list: ColumnDeclaration<Row, EntityMetadata>[]) => list;

describe("auditColumnsFor", () => {
  it("appends both timestamps to a page declaring neither", () => {
    expect(auditColumnsFor(columns({ name: "description" }), METADATA)).toEqual(
      [
        { name: "created", size: 130 },
        { name: "lastUpdate", size: 130 },
      ]
    );
  });

  it("leaves a page's own declaration alone — its width and its visibility are its own", () => {
    expect(
      auditColumnsFor(
        columns({ name: "description" }, { name: "lastUpdate", size: 130 }),
        METADATA
      )
    ).toEqual([{ name: "created", size: 130 }]);
  });

  it("appends nothing where the page declares both", () => {
    expect(
      auditColumnsFor(
        columns({ name: "created" }, { name: "lastUpdate" }),
        METADATA
      )
    ).toEqual([]);
  });

  it("skips what the entity has no field for, rather than adding an always empty column", () => {
    expect(
      auditColumnsFor(columns({ name: "description" }), WITHOUT_TIMESTAMPS)
    ).toEqual([]);
  });
});

describe("defaultVisibilityOf", () => {
  it("starts every appended column hidden", () => {
    expect(defaultVisibilityOf(auditColumnsFor(columns(), METADATA))).toEqual({
      created: false,
      lastUpdate: false,
    });
  });

  it("says nothing about a page that declares both — an empty default is 'as declared'", () => {
    expect(
      defaultVisibilityOf(
        auditColumnsFor(
          columns({ name: "created" }, { name: "lastUpdate" }),
          METADATA
        )
      )
    ).toEqual({});
  });
});
