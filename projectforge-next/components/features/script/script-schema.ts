import { z } from "zod";
import { SCRIPT_METADATA } from "@/lib/metadata/script.generated";
import { SCRIPT_PARAMETER_KEYS } from "@/lib/rs/script";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from ScriptDO through `lib/metadata/script.generated.ts`. Which fields
 * the form has mirrors org.projectforge.rest.dto.Script, which nests a parameter's three columns
 * (`parameter1Name`, `parameter1Type`, `parameter1Description`) into one object (`parameter1`).
 */
const m = fromMetadata(SCRIPT_METADATA);

/** A user or group, as the DTO carries it: the id is what `copyTo` stores. */
const ref = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

/** The declaration of one parameter; its values are only the execution page's. */
const parameter = (index: 1 | 2 | 3 | 4 | 5 | 6) =>
  z.looseObject({
    name: m.nullableString(`parameter${index}Name`),
    type: m.enumField(`parameter${index}Type`),
    description: m.nullableString(`parameter${index}Description`),
  });

export const scriptSchema = z.object({
  // null while the script is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  name: m.requiredString("name"),
  type: m.enumField("type"),
  description: m.nullableString("description"),
  // The code is no column of the metadata: ScriptDO stores it as a byte array.
  script: z.string().nullable(),
  parameter1: parameter(1),
  parameter2: parameter(2),
  parameter3: parameter(3),
  parameter4: parameter(4),
  parameter5: parameter(5),
  parameter6: parameter(6),
  // No metadata: stored as csv lists of ids, which the DTO resolves to users and groups.
  executableByGroups: z.array(ref),
  executableByUsers: z.array(ref),
  executeAsUser: m.entityField("executeAsUser"),
  created: z.string().nullable(),
});

export type ScriptValues = z.infer<typeof scriptSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees. The parameters are
 * named by their nested paths, which is where their fields are mounted.
 */
export const SCRIPT_FIELDS: readonly string[] = [
  ...Object.keys(scriptSchema.shape).filter(
    (key) => !key.startsWith("parameter")
  ),
  ...SCRIPT_PARAMETER_KEYS.flatMap((key) => [
    `${key}.name`,
    `${key}.type`,
    `${key}.description`,
  ]),
];
