import { describe, expect, it } from "vitest";
import {
  readArrayAtPath,
  removeRow,
  restoreRow,
  updateRow,
} from "./field-array";

describe("readArrayAtPath", () => {
  it("reads a collection of the entity itself", () => {
    const values = { positionen: [{ id: 1 }, { id: 2 }] };
    expect(readArrayAtPath(values, "positionen")).toHaveLength(2);
  });

  it("reads a collection nested inside a row of another one", () => {
    // The second nesting level of the invoice form. A flat values[name] lookup answers nothing here,
    // which is what this whole helper exists for.
    const values = {
      positionen: [
        { id: 1, kostZuweisungen: [{ id: 11 }] },
        { id: 2, kostZuweisungen: [{ id: 21 }, { id: 22 }] },
      ],
    };
    expect(readArrayAtPath(values, "positionen[1].kostZuweisungen")).toEqual([
      { id: 21 },
      { id: 22 },
    ]);
  });

  it("answers an empty array at any missing hop, rather than throwing", () => {
    // Every one of these happens while a form is being edited: a fresh invoice has no positions, a
    // fresh position no assignments, and a row is removed while its section still renders once more.
    expect(readArrayAtPath(undefined, "positionen")).toEqual([]);
    expect(readArrayAtPath({}, "positionen[0].kostZuweisungen")).toEqual([]);
    expect(
      readArrayAtPath(
        { positionen: [{ id: 1 }] },
        "positionen[0].kostZuweisungen"
      )
    ).toEqual([]);
    expect(
      readArrayAtPath({ positionen: [] }, "positionen[3].kostZuweisungen")
    ).toEqual([]);
  });

  it("answers an empty array where the path leads to something that is no array", () => {
    expect(readArrayAtPath({ betreff: "Text" }, "betreff")).toEqual([]);
  });
});

describe("removeRow", () => {
  it("marks a stored row deleted instead of dropping it", () => {
    // Dropping it would delete it physically in the backend, history and all: neither
    // RechnungDO.positionen nor AuftragDO.positionen has @SoftDeleteCollection.
    const rows = [{ id: 7 }, { id: 8 }];
    expect(removeRow(rows, 0)).toEqual([{ id: 7, deleted: true }, { id: 8 }]);
  });

  it("drops a row that was never saved", () => {
    // There is nothing in the database to soft-delete, and keeping it would post an empty row.
    const rows = [{ id: 7 }, { id: null }];
    expect(removeRow(rows, 1)).toEqual([{ id: 7 }]);
  });

  it("treats a row without an id property as unsaved", () => {
    expect(removeRow([{}], 0)).toEqual([]);
  });

  it("answers the same rows for an index that is not there", () => {
    const rows = [{ id: 7 }];
    expect(removeRow(rows, 5)).toBe(rows);
  });
});

describe("restoreRow", () => {
  it("takes a deleted row back", () => {
    expect(restoreRow([{ id: 7, deleted: true }], 0)).toEqual([
      { id: 7, deleted: false },
    ]);
  });
});

describe("updateRow", () => {
  it("merges the changes into one row and leaves the others alone", () => {
    const rows = [
      { id: 7, netto: "100" },
      { id: 8, netto: "200" },
    ];
    expect(updateRow(rows, 1, { netto: "300" })).toEqual([
      { id: 7, netto: "100" },
      { id: 8, netto: "300" },
    ]);
  });
});
