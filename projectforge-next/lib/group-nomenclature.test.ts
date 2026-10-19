import { describe, expect, it } from "vitest";
import {
  groupBaseName,
  parseGroupNomenclatureConfig,
  suggestedGroupName,
} from "./group-nomenclature";

const micromata = {
  pattern: "{customer.identifier}-{project.identifier}",
  managerSuffix: "pm",
  externalSuffix: "extern",
};
const customer = { nummer: 123, name: "ACME AG", identifier: "ACME" };
const project = { nummer: 4, name: "Portal relaunch", identifier: "PORTAL" };

describe("groupBaseName", () => {
  it("fills the configured pattern", () => {
    expect(groupBaseName(micromata, { customer, project })).toBe("ACME-PORTAL");
  });

  it("falls back to the name for an empty identifier", () => {
    expect(
      groupBaseName(micromata, {
        customer: { ...customer, identifier: " " },
        project,
      })
    ).toBe("ACME AG-PORTAL");
  });

  it("drops hyphens left over by empty placeholders", () => {
    expect(groupBaseName(micromata, { project })).toBe("PORTAL");
    expect(groupBaseName(micromata, {})).toBeNull();
  });

  it("takes the project's name without a pattern", () => {
    expect(groupBaseName({}, { customer, project })).toBe("Portal relaunch");
    expect(groupBaseName(null, { customer, project })).toBe("Portal relaunch");
  });

  it("fills the numbers", () => {
    expect(
      groupBaseName(
        { pattern: "{customer.number}.{project.number}" },
        { customer, project }
      )
    ).toBe("123.4");
  });
});

describe("suggestedGroupName", () => {
  it("appends the configured suffix", () => {
    expect(
      suggestedGroupName(micromata, "managerGroup", "ACME-PORTAL", "x")
    ).toBe("ACME-PORTAL-pm");
    expect(
      suggestedGroupName(micromata, "externalGroup", "ACME-PORTAL", "external")
    ).toBe("ACME-PORTAL-extern");
    expect(suggestedGroupName(micromata, "team", "ACME-PORTAL", null)).toBe(
      "ACME-PORTAL"
    );
  });

  it("keeps the default suffix without a configured one", () => {
    expect(suggestedGroupName({}, "managerGroup", "Portal", "pm")).toBe(
      "Portal-pm"
    );
  });

  it("applies the configured letter case to the whole name", () => {
    expect(
      suggestedGroupName(
        { ...micromata, letterCase: "LOWER" },
        "managerGroup",
        "ACME-PORTAL",
        null
      )
    ).toBe("acme-portal-pm");
    expect(
      suggestedGroupName({ letterCase: "UPPER" }, "team", "Portal", null)
    ).toBe("PORTAL");
  });

  it("suggests nothing without a base name", () => {
    expect(suggestedGroupName(micromata, "team", " ", null)).toBeNull();
  });
});

describe("parseGroupNomenclatureConfig", () => {
  it("parses an object and rejects anything else", () => {
    expect(parseGroupNomenclatureConfig(JSON.stringify(micromata))).toEqual({
      config: micromata,
      invalid: false,
    });
    expect(parseGroupNomenclatureConfig("")).toEqual({
      config: {},
      invalid: false,
    });
    expect(parseGroupNomenclatureConfig("[1]").invalid).toBe(true);
    expect(parseGroupNomenclatureConfig("{").invalid).toBe(true);
  });
});
