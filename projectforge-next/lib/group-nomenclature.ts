/**
 * The naming convention of groups, the JSON value of the configuration parameter `groupNomenclature`
 * (`GroupNomenclatureConfig`, projectforge-business). Only a description: the wizards suggest names
 * following it, nothing is validated against it.
 */
export const GROUP_NOMENCLATURE_PARAM = "groupNomenclature";

export interface GroupNomenclatureConfig {
  /** Rich text (HTML) explaining the convention, shown where groups are named. */
  description?: string | null;
  /** The base name of a project's groups, see [GROUP_NOMENCLATURE_PLACEHOLDERS]; null: the project's name. */
  pattern?: string | null;
  /** Null keeps the wizard's default (the translated `groupNameSuffix`). */
  managerSuffix?: string | null;
  teamSuffix?: string | null;
  externalSuffix?: string | null;
  /** The case of the whole suggested name, suffix included; null keeps it as built. */
  letterCase?: GroupNomenclatureLetterCase | null;
}

export type GroupNomenclatureLetterCase = "LOWER" | "UPPER";

/** The role of a group, as the wizards' steps are keyed (`WizardGroupKey`). */
export type GroupNomenclatureRole = "managerGroup" | "team" | "externalGroup";

/** The suffix field of each role. */
const SUFFIX_FIELDS: Record<
  GroupNomenclatureRole,
  "managerSuffix" | "teamSuffix" | "externalSuffix"
> = {
  managerGroup: "managerSuffix",
  team: "teamSuffix",
  externalGroup: "externalSuffix",
};

/** What a placeholder is filled from: a customer or a project. */
export interface NomenclatureEntity {
  nummer?: number | string | null;
  name?: string | null;
  identifier?: string | null;
}

/** The placeholders of the pattern, in the order the editor lists them. */
export const GROUP_NOMENCLATURE_PLACEHOLDERS = [
  "{customer.identifier}",
  "{customer.name}",
  "{customer.number}",
  "{project.identifier}",
  "{project.name}",
  "{project.number}",
] as const;

function placeholderValue(
  entity: NomenclatureEntity | null | undefined,
  field: string
): string {
  if (!entity) return "";
  switch (field) {
    case "identifier":
      // An entity without an identifier is still named: by its name.
      return (entity.identifier?.trim() || entity.name?.trim()) ?? "";
    case "name":
      return entity.name?.trim() ?? "";
    case "number":
      return entity.nummer == null ? "" : String(entity.nummer).trim();
    default:
      return "";
  }
}

/**
 * The base name of a project's groups: the configured pattern with its placeholders filled in, or the
 * project's name without one. Hyphens left over by empty placeholders are dropped. Null if nothing is
 * left, e.g. as long as the project has no name.
 */
export function groupBaseName(
  config: GroupNomenclatureConfig | null | undefined,
  context: {
    customer?: NomenclatureEntity | null;
    project?: NomenclatureEntity | null;
  }
): string | null {
  const pattern = config?.pattern?.trim() || "{project.name}";
  const filled = pattern.replace(
    /\{(customer|project)\.(identifier|name|number)\}/g,
    (_, entity: string, field: string) =>
      placeholderValue(
        entity === "customer" ? context.customer : context.project,
        field
      )
  );
  const name = filled
    .replace(/-{2,}/g, "-")
    .replace(/^[\s-]+|[\s-]+$/g, "")
    .trim();
  return name || null;
}

/**
 * The suggested name of a role's group: the base name plus the role's suffix, joined by a hyphen. The
 * configured suffix wins; without one the wizard's own `defaultSuffix` applies (none for the team).
 * A configured letter case applies to the whole name.
 */
export function suggestedGroupName(
  config: GroupNomenclatureConfig | null | undefined,
  role: GroupNomenclatureRole,
  baseName: string | null | undefined,
  defaultSuffix: string | null | undefined
): string | null {
  const base = baseName?.trim();
  if (!base) return null;
  const configured = config?.[SUFFIX_FIELDS[role]];
  const suffix = (configured ?? defaultSuffix)?.trim();
  const name = suffix ? `${base}-${suffix}` : base;
  switch (config?.letterCase) {
    case "LOWER":
      return name.toLowerCase();
    case "UPPER":
      return name.toUpperCase();
    default:
      return name;
  }
}

/**
 * The stored value as settings. `invalid` if the value isn't a JSON object; the defaults (no settings)
 * are taken then, as the backend does.
 */
export function parseGroupNomenclatureConfig(json: string | null | undefined): {
  config: GroupNomenclatureConfig;
  invalid: boolean;
} {
  if (!json?.trim()) return { config: {}, invalid: false };
  try {
    const value: unknown = JSON.parse(json);
    if (typeof value !== "object" || value === null || Array.isArray(value)) {
      return { config: {}, invalid: true };
    }
    return { config: value as GroupNomenclatureConfig, invalid: false };
  } catch {
    return { config: {}, invalid: true };
  }
}
