import type {
  PropertyChange,
  PropertyGroup,
  PropertyParam,
} from "@/lib/rs/properties-editor";

/** The not yet saved edit of one parameter: a new value, or removing it from the file. */
export type PropertyEdit = { kind: "set"; value: string } | { kind: "remove" };

export type PropertyEdits = Record<string, PropertyEdit>;

export interface PropertyFilter {
  query: string;
  onlyInFile: boolean;
  onlyModified: boolean;
}

/** Id of the backend's group of keys unknown to ProjectForge (ProjectForgePropertiesService). */
export const OTHER_GROUP_ID = "other";

/** Id of the backend's group of keys in ProjectForge's namespace it doesn't know (any more). */
export const OBSOLETE_GROUP_ID = "obsolete";

/** Undocumented keys aren't reset to a default but deleted from the file. */
export function isUndocumented(param: PropertyParam): boolean {
  return param.status === "OBSOLETE" || param.status === "UNKNOWN";
}

const KEY_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._\-[\]]*$/;

export function isValidKey(key: string): boolean {
  return KEY_PATTERN.test(key);
}

/** The value the input shows: the edit, else the value of the file; null if not set in the file. */
export function currentValue(
  param: PropertyParam,
  edit: PropertyEdit | undefined
): string | null {
  if (edit?.kind === "set") return edit.value;
  if (edit?.kind === "remove") return null;
  return param.inFile ? (param.fileValue ?? "") : null;
}

/** Whether the edit changes the file at all (setting the value it already has is no change). */
export function isEffectiveEdit(
  param: PropertyParam,
  edit: PropertyEdit | undefined
): boolean {
  if (!edit) return false;
  if (edit.kind === "remove") return param.inFile;
  // A secret's value is unknown here: entering one is always a change.
  if (param.secret) return true;
  return !param.inFile || (param.fileValue ?? "") !== edit.value;
}

export function buildChanges(
  params: PropertyParam[],
  edits: PropertyEdits
): PropertyChange[] {
  return params
    .filter((param) => isEffectiveEdit(param, edits[param.key]))
    .map((param) => {
      const edit = edits[param.key];
      return edit.kind === "remove"
        ? { key: param.key, remove: true }
        : { key: param.key, value: edit.value, remove: false };
    });
}

export function allParams(groups: PropertyGroup[]): PropertyParam[] {
  return groups.flatMap((group) =>
    group.blocks.flatMap((block) => block.params)
  );
}

/** A parameter added by the admin, not yet in the file (and unknown to application.properties). */
export function newParam(key: string): PropertyParam {
  return {
    key,
    optional: true,
    known: false,
    status: "UNKNOWN",
    type: "STRING",
    secret: false,
    inFile: false,
    activeDiffers: false,
  };
}

/** The groups with the parameters added by the admin in the group "Other". */
export function withAddedParams(
  groups: PropertyGroup[],
  addedKeys: string[]
): PropertyGroup[] {
  if (addedKeys.length === 0) return groups;
  const added = { description: null, params: addedKeys.map(newParam) };
  const other = groups.find((group) => group.id === OTHER_GROUP_ID);
  if (other) {
    return groups.map((group) =>
      group === other ? { ...group, blocks: [...group.blocks, added] } : group
    );
  }
  return [
    ...groups,
    {
      id: OTHER_GROUP_ID,
      title: OTHER_GROUP_ID,
      expert: false,
      blocks: [added],
    },
  ];
}

function matchesQuery(
  param: PropertyParam,
  blockDescription: string | null | undefined,
  words: string[]
): boolean {
  const text = [
    param.key,
    param.description,
    blockDescription,
    param.fileComment,
  ]
    .filter(Boolean)
    .join("\n")
    .toLowerCase();
  return words.every((word) => text.includes(word));
}

/** The groups and blocks reduced to the matching parameters, empty ones dropped. */
export function filterGroups(
  groups: PropertyGroup[],
  filter: PropertyFilter,
  edits: PropertyEdits
): PropertyGroup[] {
  const words = filter.query.toLowerCase().split(/\s+/).filter(Boolean);
  return groups
    .map((group) => ({
      ...group,
      blocks: group.blocks
        .map((block) => ({
          ...block,
          params: block.params.filter(
            (param) =>
              (!filter.onlyInFile || param.inFile) &&
              (!filter.onlyModified ||
                isEffectiveEdit(param, edits[param.key])) &&
              (words.length === 0 ||
                matchesQuery(param, block.description, words) ||
                group.title.toLowerCase().includes(words.join(" ")))
          ),
        }))
        .filter((block) => block.params.length > 0),
    }))
    .filter((group) => group.blocks.length > 0);
}
