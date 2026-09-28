import { z } from "zod";
import { CONFIGURATION_TYPES } from "./types";

/**
 * Mirrors org.projectforge.rest.dto.Configuration. Entirely hand-written: the value of a configuration
 * parameter is polymorphic (held per `configurationType` in one of four typed slots), so the DO carries
 * no single `@PropertyInfo` value field the generated metadata could describe — CONFIGURATION_METADATA
 * has only the base columns. `parameter` and `configurationType` are fixed identity of the row and
 * never edited; the four value slots are all nullable, and the backend writes only the one that matches
 * the type (`Configuration.copyTo`).
 */
export const configurationSchema = z.object({
  // Never null in practice — the parameter set is fixed and every row already exists. Nullable only to
  // satisfy the add contract the generic edit page shares (add is unreachable, DAO refuses insert).
  id: z.number().nullable(),
  parameter: z.string().nullable(),
  configurationType: z.enum(CONFIGURATION_TYPES).nullable(),
  i18nKey: z.string().nullable(),
  descriptionI18nKey: z.string().nullable(),
  stringValue: z.string().nullable(),
  longValue: z.number().nullable(),
  floatValue: z.number().nullable(),
  booleanValue: z.boolean().nullable(),
});

export type ConfigurationValues = z.infer<typeof configurationSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const CONFIGURATION_FIELDS = Object.keys(
  configurationSchema.shape
) as readonly (keyof ConfigurationValues)[];
