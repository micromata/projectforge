import { describe, expect, it } from "vitest";
import type { FilterElement } from "@/lib/rs/types";
import {
  ACTIVE_GROUP_ID,
  PLAIN_GROUP_ID,
  TECHNICAL_GROUP_ID,
  buildFilterGroups,
  controlRankOf,
  fieldLabelInGroup,
  filterGroupsBySearch,
  groupLabelOf,
  isTechnicalField,
} from "./filter-groups";

function element(
  id: string,
  overrides: Partial<FilterElement> = {}
): FilterElement {
  return { id, label: id, filterType: "STRING", ...overrides } as FilterElement;
}

/** A grouped field as the current backend sends it. */
function nested(id: string, group: string, shortLabel: string): FilterElement {
  return element(id, { group, shortLabel, label: `${group} - ${shortLabel}` });
}

/** The same field from a backend that predates UIFilterElement.group. */
function legacyNested(id: string, label: string): FilterElement {
  return element(id, { label });
}

describe("groupLabelOf", () => {
  it("takes the group the backend sends", () => {
    expect(groupLabelOf(nested("kunde.name", "Kunde", "Name"))).toBe("Kunde");
  });

  it("falls back to the label prefix of an older backend", () => {
    expect(groupLabelOf(legacyNested("kunde.name", "Kunde - Name"))).toBe(
      "Kunde"
    );
    expect(
      groupLabelOf(legacyNested("projekt.kunde.name", "Projekt - Kunde - Name"))
    ).toBe("Projekt - Kunde");
  });

  it("is null for a field of the entity itself", () => {
    expect(groupLabelOf(element("titel", { label: "Titel" }))).toBeNull();
  });
});

describe("isTechnicalField", () => {
  it("takes the backend's answer, even when it says no", () => {
    expect(
      isTechnicalField(element("attachmentsIds", { technical: true }))
    ).toBe(true);
    expect(
      isTechnicalField(element("hasAttachments", { technical: false }))
    ).toBe(false);
  });

  it("recognizes an untranslated field of an older backend by label === id", () => {
    expect(isTechnicalField(element("attachmentsNames"))).toBe(true);
    expect(isTechnicalField(element("titel", { label: "Titel" }))).toBe(false);
  });
});

describe("fieldLabelInGroup", () => {
  it("uses the short label", () => {
    expect(fieldLabelInGroup(nested("kunde.name", "Kunde", "Name"))).toBe(
      "Name"
    );
  });

  it("strips the group prefix an older backend only has in the label", () => {
    expect(fieldLabelInGroup(legacyNested("kunde.name", "Kunde - Name"))).toBe(
      "Name"
    );
  });

  it("keeps a label that isn't prefixed, and falls back to the id", () => {
    expect(fieldLabelInGroup(element("titel", { label: "Titel" }))).toBe(
      "Titel"
    );
    expect(fieldLabelInGroup(element("titel", { label: undefined }))).toBe(
      "titel"
    );
  });
});

describe("buildFilterGroups", () => {
  const elements = [
    element("attachmentsIds", { technical: true }),
    nested("contactPerson.firstname", "Ansprechpartner:in", "Vorname"),
    nested("contactPerson.name", "Ansprechpartner:in", "Name"),
    element("deleted", { label: "Gelöscht", filterType: "BOOLEAN" }),
    nested("kunde.name", "Kunde", "Name"),
    element("status", { label: "Status", filterType: "LIST" }),
    element("titel", { label: "Titel" }),
  ];

  it("groups the fields in reading order: active, own, nested, technical", () => {
    const groups = buildFilterGroups(elements, { titel: { value: "x" } });
    expect(groups.map((group) => group.id)).toEqual([
      ACTIVE_GROUP_ID,
      PLAIN_GROUP_ID,
      "group:Ansprechpartner:in",
      "group:Kunde",
      TECHNICAL_GROUP_ID,
    ]);
    expect(groups[0].elements.map((e) => e.id)).toEqual(["titel"]);
    expect(groups[1].elements.map((e) => e.id)).toEqual(["deleted", "status"]);
    expect(groups[4].elements.map((e) => e.id)).toEqual(["attachmentsIds"]);
  });

  it("shows an active field only among the active ones, not in its own group too", () => {
    const groups = buildFilterGroups(elements, {
      "kunde.name": { value: "Acme" },
    });
    expect(groups.find((group) => group.id === "group:Kunde")).toBeUndefined();
    expect(groups[0].elements.map((e) => e.id)).toEqual(["kunde.name"]);
  });

  it("counts a default filter as active, valueless as it is", () => {
    const withDefault = [...elements, element("year", { defaultFilter: true })];
    const groups = buildFilterGroups(withDefault, {});
    expect(groups[0].elements.map((e) => e.id)).toEqual(["year"]);
  });

  it("leaves out the groups an entity has nothing for", () => {
    const groups = buildFilterGroups(
      [element("titel", { label: "Titel" })],
      {}
    );
    expect(groups.map((group) => group.id)).toEqual([PLAIN_GROUP_ID]);
  });

  it("groups an older backend's fields by their label prefix", () => {
    const groups = buildFilterGroups(
      [
        legacyNested("kunde.name", "Kunde - Name"),
        legacyNested("kunde.division", "Kunde - Bereich"),
        element("attachmentsNames"),
      ],
      {}
    );
    expect(groups.map((group) => group.id)).toEqual([
      "group:Kunde",
      TECHNICAL_GROUP_ID,
    ]);
  });
});

describe("filterGroupsBySearch", () => {
  const groups = buildFilterGroups(
    [
      element("attachmentsIds", { technical: true }),
      nested("kunde.name", "Kunde", "Name"),
      nested("kunde.division", "Kunde", "Bereich"),
      element("titel", { label: "Titel" }),
    ],
    {}
  );

  it("returns everything for an empty term", () => {
    expect(filterGroupsBySearch(groups, "  ")).toBe(groups);
  });

  it("keeps a whole group when its heading matches", () => {
    const found = filterGroupsBySearch(groups, "kund");
    expect(found).toHaveLength(1);
    expect(found[0].elements.map((e) => e.id)).toEqual([
      "kunde.name",
      "kunde.division",
    ]);
  });

  it("keeps only the matching fields of a group", () => {
    const found = filterGroupsBySearch(groups, "bereich");
    expect(found.map((group) => group.elements.map((e) => e.id))).toEqual([
      ["kunde.division"],
    ]);
  });

  it("finds an untranslated field by its raw id", () => {
    const found = filterGroupsBySearch(groups, "attachments");
    expect(found.map((group) => group.elements.map((e) => e.id))).toEqual([
      ["attachmentsIds"],
    ]);
  });

  it("drops every group when nothing matches", () => {
    expect(filterGroupsBySearch(groups, "xyz")).toEqual([]);
  });
});

describe("controlRankOf", () => {
  // LIST counts as flat: [ListField] is a combobox, one line tall whatever its option count.
  it("orders the fields from the flattest input to the tallest", () => {
    const ranks = (
      ["BOOLEAN", "STRING", "OBJECT", "LIST", "DATE", "TIMESTAMP"] as const
    ).map((filterType) => controlRankOf(element("x", { filterType })));
    expect(ranks).toEqual([0, 1, 1, 1, 2, 3]);
  });
});
