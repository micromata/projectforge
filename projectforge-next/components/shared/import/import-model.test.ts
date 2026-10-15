import { describe, expect, it } from "vitest";
import { formatCurrency, formatDate, type FormatContext } from "@/lib/format";
import {
  diffOf,
  filterEntriesByStatus,
  formatByKind,
  isSelectable,
  IMPORTABLE_STATUSES,
  rowClassForStatus,
  selectableIds,
  statisticsEntries,
  STATUS_GROUP_OF,
  tooltipOf,
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

describe("tooltipOf", () => {
  const column: ImportColumn = {
    field: "kost2",
    headerKey: "fibu.kost2",
    kind: "text",
    tooltipField: "kost2Info",
  };

  it("reads the tooltip text by its own path", () => {
    const row = entry("NEW", {
      read: { kost2: "5.000.01.01", kost2Info: "Wartung\nACME - Portal" },
    });
    expect(tooltipOf(row, column)).toBe("Wartung\nACME - Portal");
  });

  it("yields nothing for an absent or blank text, or a column without tooltipField", () => {
    expect(tooltipOf(entry("NEW", { read: { kost2: "x" } }), column)).toBe(
      undefined
    );
    expect(tooltipOf(entry("NEW", { read: { kost2Info: "  " } }), column)).toBe(
      undefined
    );
    const plain: ImportColumn = { ...column, tooltipField: undefined };
    expect(
      tooltipOf(entry("NEW", { read: { kost2Info: "Wartung" } }), plain)
    ).toBe(undefined);
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

  it("renders a month as its two-digit number, locale-independent", () => {
    expect(formatByKind(9, "month", ctx)).toBe("09");
    expect(formatByKind(12, "month", ctx)).toBe("12");
  });

  it("renders an integer without a thousands separator (a year or an id, not a quantity)", () => {
    expect(formatByKind(2026, "integer", ctx)).toBe("2026");
    expect(formatByKind(1001, "integer", ctx)).toBe("1001");
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

describe("STATUS_GROUP_OF", () => {
  it("maps every status to a chip group, folding both unknown states into one", () => {
    expect(STATUS_GROUP_OF.NEW).toBe("new");
    expect(STATUS_GROUP_OF.MODIFIED).toBe("modified");
    expect(STATUS_GROUP_OF.DELETED).toBe("deleted");
    expect(STATUS_GROUP_OF.UNMODIFIED).toBe("unmodified");
    expect(STATUS_GROUP_OF.IMPORTED).toBe("imported");
    expect(STATUS_GROUP_OF.FAULTY).toBe("faulty");
    expect(STATUS_GROUP_OF.UNKNOWN).toBe("unknown");
    expect(STATUS_GROUP_OF.UNKNOWN_MODIFICATION).toBe("unknown");
  });
});

describe("filterEntriesByStatus", () => {
  const entries: ImportEntry[] = [
    entry("NEW", { id: 1 }),
    entry("UNMODIFIED", { id: 2 }),
    entry("MODIFIED", { id: 3 }),
    entry("FAULTY", { id: 4 }),
  ];

  it("returns the entries untouched when nothing is hidden", () => {
    expect(filterEntriesByStatus(entries, new Set())).toBe(entries);
  });

  it("drops the rows whose group key is hidden", () => {
    expect(
      filterEntriesByStatus(entries, new Set(["unmodified"])).map((e) => e.id)
    ).toEqual([1, 3, 4]);
    expect(
      filterEntriesByStatus(entries, new Set(["unmodified", "faulty"])).map(
        (e) => e.id
      )
    ).toEqual([1, 3]);
  });

  it("folds both unknown states into the one 'unknown' key", () => {
    const withUnknowns: ImportEntry[] = [
      entry("UNKNOWN", { id: 5 }),
      entry("UNKNOWN_MODIFICATION", { id: 6 }),
      entry("NEW", { id: 7 }),
    ];
    expect(
      filterEntriesByStatus(withUnknowns, new Set(["unknown"])).map((e) => e.id)
    ).toEqual([7]);
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
