import { describe, expect, it } from "vitest";
import type { PropertyGroup, PropertyParam } from "@/lib/rs/properties-editor";
import {
  buildChanges,
  currentValue,
  filterGroups,
  isValidKey,
  newParam,
  withAddedParams,
} from "./properties-editor-model";

const param = (
  key: string,
  extra: Partial<PropertyParam> = {}
): PropertyParam => ({
  ...newParam(key),
  known: true,
  ...extra,
});

const groups: PropertyGroup[] = [
  {
    id: "general",
    title: "General",
    expert: false,
    blocks: [
      {
        description: "The external url",
        params: [
          param("projectforge.domain", {
            inFile: true,
            fileValue: "https://pf",
          }),
        ],
      },
      { description: null, params: [param("projectforge.support.mail")] },
    ],
  },
  {
    id: "database",
    title: "Database",
    expert: false,
    blocks: [
      {
        description: null,
        params: [
          param("spring.datasource.password", { secret: true, inFile: true }),
        ],
      },
    ],
  },
];

describe("properties editor model", () => {
  it("shows the edit, else the file value", () => {
    const domain = groups[0].blocks[0].params[0];
    expect(currentValue(domain, undefined)).toBe("https://pf");
    expect(currentValue(domain, { kind: "set", value: "x" })).toBe("x");
    expect(currentValue(domain, { kind: "remove" })).toBeNull();
    expect(currentValue(groups[0].blocks[1].params[0], undefined)).toBeNull();
  });

  it("builds only effective changes", () => {
    const params = groups.flatMap((g) => g.blocks.flatMap((b) => b.params));
    const changes = buildChanges(params, {
      "projectforge.domain": { kind: "set", value: "https://pf" }, // unchanged
      "projectforge.support.mail": { kind: "remove" }, // not in file
      "spring.datasource.password": { kind: "set", value: "secret" },
    });
    expect(changes).toEqual([
      { key: "spring.datasource.password", value: "secret", remove: false },
    ]);
    expect(
      buildChanges(params, { "projectforge.domain": { kind: "remove" } })
    ).toEqual([{ key: "projectforge.domain", remove: true }]);
  });

  it("filters by query, file and modification", () => {
    const byQuery = filterGroups(
      groups,
      { query: "external", onlyInFile: false, onlyModified: false },
      {}
    );
    expect(byQuery.map((g) => g.id)).toEqual(["general"]);
    expect(byQuery[0].blocks).toHaveLength(1);
    const inFile = filterGroups(
      groups,
      { query: "", onlyInFile: true, onlyModified: false },
      {}
    );
    expect(
      inFile.flatMap((g) => g.blocks.flatMap((b) => b.params.map((p) => p.key)))
    ).toEqual(["projectforge.domain", "spring.datasource.password"]);
    const modified = filterGroups(
      groups,
      { query: "", onlyInFile: false, onlyModified: true },
      { "projectforge.support.mail": { kind: "set", value: "a@b.c" } }
    );
    expect(
      modified.flatMap((g) =>
        g.blocks.flatMap((b) => b.params.map((p) => p.key))
      )
    ).toEqual(["projectforge.support.mail"]);
  });

  it("adds new keys to the group Other", () => {
    const result = withAddedParams(groups, ["my.key"]);
    expect(result.at(-1)?.id).toBe("other");
    expect(result.at(-1)?.blocks[0].params[0].key).toBe("my.key");
    expect(isValidKey("my.key[0].x-y_z")).toBe(true);
    expect(isValidKey("my key")).toBe(false);
    expect(isValidKey(".key")).toBe(false);
  });
});
