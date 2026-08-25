import { describe, expect, it } from "vitest";
import { formatCurrency, formatDate, type FormatContext } from "@/lib/format";
import {
  diffOf,
  formatByKind,
  isSelectable,
  IMPORTABLE_STATUSES,
  rowClassForStatus,
  selectableIds,
  statisticsEntries,
  visibleColumns,
} from "./import-model";
import type {
  ImportColumn,
  ImportEntry,
  ImportStatus,
  ImportStorageInfo,
} from "./import-types";

const ctx: FormatContext = {
  locale: "de-DE",
  timeZone: "Europe/Berlin",
  currency: "EUR",
};

/** A minimal entry — only the fields the model reads. */
function entry(
  status: ImportStatus,
  overrides: Partial<ImportEntry> = {}
): ImportEntry {
  return {
    id: 1,
    status,
    statusAsString: status,
    hasError: false,
    ...overrides,
  };
}

describe("rowClassForStatus", () => {
  it("tints new green, modified blue, and every removed/broken state red", () => {
    expect(rowClassForStatus("NEW")).toBe("row-green");
    expect(rowClassForStatus("MODIFIED")).toBe("row-blue");
    for (const status of [
      "DELETED",
      "FAULTY",
      "UNKNOWN",
      "UNKNOWN_MODIFICATION",
    ] as ImportStatus[]) {
      expect(rowClassForStatus(status), status).toBe("row-red");
    }
  });

  it("leaves the settled states (unmodified, imported) untinted", () => {
    expect(rowClassForStatus("UNMODIFIED")).toBeUndefined();
    expect(rowClassForStatus("IMPORTED")).toBeUndefined();
  });
});

describe("isSelectable", () => {
  it("ticks only the importable states by default — the ones the backend commit keeps", () => {
    expect(IMPORTABLE_STATUSES).toEqual(["NEW", "MODIFIED", "DELETED"]);
    expect(isSelectable("NEW")).toBe(true);
    expect(isSelectable("MODIFIED")).toBe(true);
    expect(isSelectable("DELETED")).toBe(true);
    expect(isSelectable("UNMODIFIED")).toBe(false);
    expect(isSelectable("UNKNOWN")).toBe(false);
    expect(isSelectable("FAULTY")).toBe(false);
  });

  it("honours an explicit selectableStatuses list", () => {
    expect(isSelectable("UNMODIFIED", ["UNMODIFIED"])).toBe(true);
    expect(isSelectable("NEW", ["UNMODIFIED"])).toBe(false);
  });
});

describe("visibleColumns", () => {
  const columns: ImportColumn[] = [
    { field: "always", headerKey: "a", kind: "text" },
    {
      field: "posOnly",
      headerKey: "b",
      kind: "text",
      showIf: (m) => m.isPositionBasedImport === true,
    },
    {
      field: "headerOnly",
      headerKey: "c",
      kind: "text",
      showIf: (m) => m.isPositionBasedImport === false,
    },
  ];

  it("keeps ungated columns and the gated ones whose predicate passes", () => {
    expect(
      visibleColumns(columns, { isPositionBasedImport: true }).map(
        (c) => c.field
      )
    ).toEqual(["always", "posOnly"]);
    expect(
      visibleColumns(columns, { isPositionBasedImport: false }).map(
        (c) => c.field
      )
    ).toEqual(["always", "headerOnly"]);
  });
});

describe("diffOf", () => {
  const column: ImportColumn = {
    field: "konto.nummer",
    headerKey: "fibu.konto",
    kind: "text",
    diff: true,
  };

  it("reads the current value by dotted path and the old one from the read.-prefixed key", () => {
    const modified = entry("MODIFIED", {
      read: { konto: { nummer: 1300 } },
      oldDiffValues: { "read.konto.nummer": 1200 },
    });
    const { current, old, hasDiff } = diffOf(modified, column);
    expect(current).toBe(1300);
    expect(old).toBe(1200);
    expect(hasDiff).toBe(true);
  });

  it("reports no diff when the property is absent from oldDiffValues, even on a modified row", () => {
    const modified = entry("MODIFIED", {
      read: { konto: { nummer: 1300 } },
      oldDiffValues: { "read.kreditor": "ACME" },
    });
    expect(diffOf(modified, column).hasDiff).toBe(false);
  });

  it("reports no diff for a new row (oldDiffValues is null) and still yields the current value", () => {
    const created = entry("NEW", { read: { konto: { nummer: 1300 } } });
    const { current, hasDiff } = diffOf(created, column);
    expect(current).toBe(1300);
    expect(hasDiff).toBe(false);
  });

  it("keys strictly on read.<field> — a bare property name is not the diff key", () => {
    const modified = entry("MODIFIED", {
      read: { positionNummer: 2 },
      oldDiffValues: { positionNummer: 1 }, // wrong shape: no read. prefix
    });
    const col: ImportColumn = {
      field: "positionNummer",
      headerKey: "label.position.short",
      kind: "number",
      diff: true,
    };
    expect(diffOf(modified, col).hasDiff).toBe(false);
  });
});

describe("formatByKind", () => {
  it("dispatches to the locale formatter of the column's kind", () => {
    expect(formatByKind(1234.5, "currency", ctx)).toBe(
      formatCurrency(1234.5, ctx)
    );
    expect(formatByKind("2026-08-24", "date", ctx)).toBe(
      formatDate("2026-08-24", ctx)
    );
    expect(formatByKind("ACME GmbH", "text", ctx)).toBe("ACME GmbH");
  });
});

describe("statisticsEntries", () => {
  it("is empty without info", () => {
    expect(statisticsEntries(undefined)).toEqual([]);
  });

  it("always shows the total and only the non-zero per-status counts", () => {
    const info: ImportStorageInfo = {
      totalNumber: 5,
      numberOfNewEntries: 3,
      numberOfModifiedEntries: 2,
      numberOfDeletedEntries: 0,
      numberOfUnmodifiedEntries: 0,
      numberOfUnknownEntries: 0,
      numberOfFaultyEntries: 0,
    };
    const keys = statisticsEntries(info).map((e) => e.key);
    expect(keys).toEqual(["total", "new", "modified"]);
  });
});

describe("selectableIds", () => {
  it("returns the ids of the tickable rows out of a mixed view", () => {
    const entries: ImportEntry[] = [
      entry("NEW", { id: 1 }),
      entry("UNMODIFIED", { id: 2 }),
      entry("MODIFIED", { id: 3 }),
      entry("FAULTY", { id: 4 }),
    ];
    expect(selectableIds(entries)).toEqual([1, 3]);
  });
});

// Reuse proof: the model is entity-agnostic. A synthetic address-like config — different fields,
// different gating — flows through the very same functions with no invoice knowledge involved.
describe("generic over any import config", () => {
  const addressColumns: ImportColumn[] = [
    { field: "name", headerKey: "name", kind: "text" },
    {
      field: "organization.city",
      headerKey: "address.city",
      kind: "text",
      diff: true,
      showIf: (m) => m.hasOrganization === true,
    },
  ];

  it("gates and diffs an address row exactly as it would an invoice row", () => {
    expect(
      visibleColumns(addressColumns, { hasOrganization: false }).map(
        (c) => c.field
      )
    ).toEqual(["name"]);
    const row = entry("MODIFIED", {
      read: { organization: { city: "Kassel" } },
      oldDiffValues: { "read.organization.city": "Berlin" },
    });
    expect(diffOf(row, addressColumns[1])).toEqual({
      current: "Kassel",
      old: "Berlin",
      hasDiff: true,
    });
  });
});
