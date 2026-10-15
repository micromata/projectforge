import { describe, expect, it } from "vitest";
import { applyCustomerOverrides } from "./config";

/**
 * The runtime overlay of a deployment's CustomerI18nResources onto the static catalog. A flat map of
 * dotted keys (the properties form the backend hands back) is written along its path, mirroring the
 * generator's own nesting rule so an override lands exactly where next-intl reads it.
 */
describe("applyCustomerOverrides", () => {
  it("returns the base untouched when there are no overrides", () => {
    const base = { common: { import: { action: { reconcile: "Reconcile" } } } };
    expect(applyCustomerOverrides(base, undefined)).toBe(base);
    expect(applyCustomerOverrides(base, {})).toBe(base);
  });

  it("replaces one leaf and leaves its siblings and the base itself intact", () => {
    const base = {
      common: {
        import: { action: { reconcile: "Reconcile", commit: "Commit" } },
      },
    };
    const merged = applyCustomerOverrides(base, {
      "common.import.action.reconcile": "Tindern",
    }) as typeof base;

    expect(merged.common.import.action.reconcile).toBe("Tindern");
    // Sibling kept.
    expect(merged.common.import.action.commit).toBe("Commit");
    // Base not mutated — it stays the fallback.
    expect(base.common.import.action.reconcile).toBe("Reconcile");
  });

  it("adds a wholly new dotted key by creating the intermediate namespaces", () => {
    const merged = applyCustomerOverrides(
      {},
      { "feature.newLabel": "Neu" }
    ) as {
      feature: { newLabel: string };
    };
    expect(merged.feature.newLabel).toBe("Neu");
  });

  it("keeps a leaf under _ when an override needs it to become a namespace", () => {
    // base has book.title as a plain string; an override deeper than it must not drop it.
    const merged = applyCustomerOverrides(
      { book: { title: "Title" } },
      { "book.title.add": "Add title" }
    ) as { book: { title: { _: string; add: string } } };
    expect(merged.book.title.add).toBe("Add title");
    expect(merged.book.title._).toBe("Title");
  });

  it("stores the override under _ when the key already names a namespace", () => {
    // base has book.title as both a namespace (add) — overriding the bare book.title must not clobber it.
    const merged = applyCustomerOverrides(
      { book: { title: { add: "Add" } } },
      { "book.title": "Title override" }
    ) as { book: { title: { _: string; add: string } } };
    expect(merged.book.title._).toBe("Title override");
    expect(merged.book.title.add).toBe("Add");
  });
});
