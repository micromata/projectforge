import type { MerlinVariable } from "@/lib/rs/merlin";
import type { MerlinValues } from "./merlin-schema";
import type { MerlinDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(template: MerlinDetail): MerlinValues {
  return {
    id: template.id ?? null,
    name: template.name ?? "",
    description: template.description ?? null,
    fileNamePattern: template.fileNamePattern ?? "",
    stronglyRestrictedFilenames: template.stronglyRestrictedFilenames ?? false,
    pdfExport: template.pdfExport ?? false,
    dataTransferUsage: template.dataTransferUsage ?? false,
    admins: template.admins ?? [],
    accessUsers: template.accessUsers ?? [],
    accessGroups: template.accessGroups ?? [],
    variables: (template.variables ?? []) as MerlinVariable[],
    dependentVariables: (template.dependentVariables ?? []) as MerlinVariable[],
    lastVariableUpdate: template.lastVariableUpdate ?? null,
    wordTemplateFileName: template.wordTemplateFileName ?? null,
    excelTemplateDefinitionFileName:
      template.excelTemplateDefinitionFileName ?? null,
    conditionalsMarkdown: template.conditionalsMarkdown ?? null,
    created: template.created ?? null,
  };
}

/**
 * Blank form for a template that doesn't exist yet. The backend presets the admin (the logged-in user)
 * and the file name pattern (`MerlinEntityRest.newBaseDO`), and its `/rs/merlin/newEntry` answer is what
 * the form is reset onto.
 */
export function emptyMerlinValues(): MerlinValues {
  return toFormValues({ id: null });
}
