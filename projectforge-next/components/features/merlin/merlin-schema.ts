import { z } from "zod";
import { MERLIN_TEMPLATE_METADATA } from "@/lib/metadata/merlin-template.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

/**
 * Mandatory and maximum length come from MerlinTemplateDO through
 * `lib/metadata/merlin-template.generated.ts`. Which fields the form has mirrors
 * org.projectforge.plugins.merlin.MerlinTemplate.
 */
const m = fromMetadata(MERLIN_TEMPLATE_METADATA);

/** A user or group, as the DTO carries it: the id is what `copyTo` stores. */
const ref = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

/**
 * A variable as the backend computed it (used, master variable, …): edited in the variable dialog
 * (`merlin/variable/update`), not field by field.
 */
const variable = z.looseObject({
  id: z.number().nullable().optional(),
  name: z.string(),
});

export const merlinSchema = z.object({
  // null while the template is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  name: m.requiredString("name"),
  description: m.nullableString("description"),
  fileNamePattern: m.requiredString("fileNamePattern"),
  stronglyRestrictedFilenames: m.booleanField("stronglyRestrictedFilenames"),
  pdfExport: m.booleanField("pdfExport"),
  dataTransferUsage: m.booleanField("dataTransferUsage"),
  // No metadata: stored as csv lists of ids, which the DTO resolves to users and groups.
  admins: z.array(ref),
  accessUsers: z.array(ref),
  accessGroups: z.array(ref),
  // Stored as json in the template; replaced as a whole by the variable dialog and the analysis.
  variables: z.array(variable),
  dependentVariables: z.array(variable),
  // Newer than the upload of an Excel® template definition: the variables of the form win.
  lastVariableUpdate: z.string().nullable(),
  // Shown only, renewed by the analysis of the Word® template (see useMerlinAnalysis).
  wordTemplateFileName: z.string().nullable(),
  excelTemplateDefinitionFileName: z.string().nullable(),
  conditionalsMarkdown: z.string().nullable(),
  created: z.string().nullable(),
});

export type MerlinValues = z.infer<typeof merlinSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const MERLIN_FIELDS = Object.keys(
  merlinSchema.shape
) as readonly (keyof MerlinValues)[];

/** The collections of the form, see applyServerValidationErrors. */
export const MERLIN_ARRAY_FIELDS = [
  "admins",
  "accessUsers",
  "accessGroups",
  "variables",
  "dependentVariables",
] as const;
