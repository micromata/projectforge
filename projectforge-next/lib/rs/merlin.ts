/**
 * The calls of the Merlin pages (Word® templates of the plugin merlin) besides the standard REST ones of
 * `MerlinEntityRest` (list and form): the analysis of the Word® template and the changes of a variable
 * for the form, the examples of the list, and the execution (`MerlinExecuteRest`) — a single document,
 * or a serial execution by an Excel® file.
 */

import type { Attachment } from "./attachments";
import { rawRequest, request, RsError } from "./client";
import { downloadFile, downloadPost, responseBlob, saveBlob } from "./download";
import type { ValidationError } from "./types";
import { parseContentDispositionFilename } from "@/lib/dynamic/content-disposition";

/** A user or group as the DTO references it: the id is what the backend stores. */
export type MerlinRef = {
  id: number;
  displayName?: string;
};

/** de.micromata.merlin.word.templating.VariableType. */
export type MerlinVariableType = "STRING" | "INT" | "FLOAT" | "DATE";

export const MERLIN_VARIABLE_TYPES: readonly MerlinVariableType[] = [
  "STRING",
  "INT",
  "FLOAT",
  "DATE",
];

/** org.projectforge.plugins.merlin.MerlinVariable, an input or a dependent variable of a template. */
export type MerlinVariable = {
  id?: number | null;
  name: string;
  sortName?: string | null;
  description?: string | null;
  required?: boolean;
  unique?: boolean;
  minimumValue?: number | string | null;
  maximumValue?: number | string | null;
  scale?: number | null;
  allowedValues?: string[] | null;
  type?: MerlinVariableType;
  /** The master variable of a dependent variable. */
  dependsOnName?: string | null;
  /** The values of a dependent variable, one per allowed value of its master (csv). */
  mappingValues?: string | null;
  /** Set by the backend: the template knows the variable (it's stored or found in the Word® template). */
  defined?: boolean;
  /** Set by the backend: the Word® template uses the variable. */
  used?: boolean | null;
  /** Set by the backend: other variables depend on it, or conditionals use it. */
  masterVariable?: boolean | null;
  /** Read only: the allowed values of the master of a dependent variable (csv). */
  mappingMasterValues?: string | null;
  /** Read only: the allowed values as csv. */
  allowedValuesFormatted?: string | null;
};

/** org.projectforge.plugins.merlin.MerlinTemplate, the DTO of the list and the form. */
export type MerlinTemplate = {
  id: number | null;
  name?: string | null;
  description?: string | null;
  admins?: MerlinRef[] | null;
  adminsAsString?: string | null;
  accessGroups?: MerlinRef[] | null;
  accessGroupsAsString?: string | null;
  accessUsers?: MerlinRef[] | null;
  accessUsersAsString?: string | null;
  fileNamePattern?: string | null;
  stronglyRestrictedFilenames?: boolean | null;
  pdfExport?: boolean | null;
  dataTransferUsage?: boolean | null;
  wordTemplateFileName?: string | null;
  excelTemplateDefinitionFileName?: string | null;
  lastVariableUpdate?: string | null;
  variables?: MerlinVariable[];
  dependentVariables?: MerlinVariable[];
  /** The conditionals of the Word® template as Markdown. */
  conditionalsMarkdown?: string | null;
  /** The data transfer plugin is available, so `dataTransferUsage` is offered. */
  dataTransferAvailable?: boolean | null;
  attachments?: Attachment[] | null;
  attachmentsCounter?: number | null;
  attachmentsSize?: number | null;
  attachmentsSizeFormatted?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
  deleted?: boolean;
  writeAccess?: boolean | null;
  deleteAccess?: boolean | null;
};

/** `MerlinEntityRest.VariablesResult`: what replaces the variables of the form. */
export type MerlinVariablesResult = {
  variables: MerlinVariable[];
  dependentVariables: MerlinVariable[];
  lastVariableUpdate?: string | null;
  wordTemplateFileName?: string | null;
  excelTemplateDefinitionFileName?: string | null;
  conditionalsMarkdown?: string | null;
};

/** Only what the user edits travels back: the flags the backend computes are restored by it. */
function cleanVariable(variable: MerlinVariable): MerlinVariable {
  return {
    id: variable.id,
    name: variable.name,
    sortName: variable.sortName,
    description: variable.description,
    required: variable.required,
    unique: variable.unique,
    minimumValue: variable.minimumValue,
    maximumValue: variable.maximumValue,
    scale: variable.scale,
    allowedValues: variable.allowedValues,
    type: variable.type,
    dependsOnName: variable.dependsOnName,
    mappingValues: variable.mappingValues,
    defined: variable.defined,
    used: variable.used,
    masterVariable: variable.masterVariable,
  };
}

/** The template as posted to the backend: the variables without their read only values. */
export function toMerlinPostData(template: MerlinTemplate): MerlinTemplate {
  return {
    ...template,
    variables: (template.variables ?? []).map(cleanVariable),
    dependentVariables: (template.dependentVariables ?? []).map(cleanVariable),
    // Display values only, the backend has no use for them.
    attachments: undefined,
  };
}

function postVariables(
  path: string,
  template: MerlinTemplate,
  currentVariable?: MerlinVariable
): Promise<MerlinVariablesResult> {
  const data = toMerlinPostData(template);
  return request<MerlinVariablesResult>(path, {
    method: "POST",
    body: JSON.stringify({
      data: currentVariable
        ? { ...data, currentVariable: cleanVariable(currentVariable) }
        : data,
    }),
  });
}

/**
 * Analyzes the Word® template of the form (after an upload or a deletion of an attachment): the variables
 * of the form are kept, the ones found in the template are added.
 */
export function analyzeMerlinTemplate(
  template: MerlinTemplate
): Promise<MerlinVariablesResult> {
  return postVariables("/rs/merlin/analyze", template);
}

/**
 * Applies the changes of a variable to the variables of the form (a variable without an id of the form
 * is added). Nothing is saved.
 */
export function updateMerlinVariable(
  template: MerlinTemplate,
  variable: MerlinVariable
): Promise<MerlinVariablesResult> {
  return postVariables("/rs/merlin/variable/update", template, variable);
}

/** Removes a variable from the variables of the form. Nothing is saved. */
export function deleteMerlinVariable(
  template: MerlinTemplate,
  variable: MerlinVariable
): Promise<MerlinVariablesResult> {
  return postVariables("/rs/merlin/variable/delete", template, variable);
}

/** The Excel® template definition of the variables of the form (not yet saved). */
export function exportMerlinExcelTemplate(
  template: MerlinTemplate
): Promise<void> {
  return downloadPost("/rs/merlin/exportExcelTemplate", {
    data: toMerlinPostData(template),
  });
}

export type MerlinExample = "contract" | "letter";

/** Creates an example template (with its Word® template) for the logged-in user, answers its id. */
export async function createMerlinExample(
  example: MerlinExample
): Promise<number> {
  const result = await request<{ id: number }>(
    `/rs/merlin/example/${example}`,
    { method: "POST" }
  );
  return result.id;
}

/** `MerlinExecuteRest.InputVariable`: an input of the execution form. */
export type MerlinInputVariable = {
  name: string;
  type: MerlinVariableType;
  required: boolean;
  allowedValues?: string[] | null;
  description?: string | null;
  minimumValue?: number | string | null;
  maximumValue?: number | string | null;
  scale?: number | null;
};

/** The kinds of Excel® files for a serial execution: empty, or prefilled with employees/users. */
export type MerlinSerialFill = "base" | "employees" | "users";

/** `MerlinExecuteRest.ExecutionData`. */
export type MerlinExecution = {
  id: number;
  name?: string | null;
  description?: string | null;
  variables: MerlinInputVariable[];
  /** The values the user entered last time, by variable name. */
  inputVariables?: Record<string, unknown> | null;
  pdfFormat: boolean;
  wordTemplateFileName?: string | null;
  serialFills: MerlinSerialFill[];
  editAccess: boolean;
  logViewerUrl: string;
};

export function merlinExecutionQueryKey(id: number) {
  return ["merlin", "execution", id] as const;
}

export function loadMerlinExecution(
  id: number,
  signal?: AbortSignal
): Promise<MerlinExecution> {
  return request<MerlinExecution>(
    `/rs/merlinexecution/load/${id}`,
    { method: "GET" },
    signal
  );
}

/** The values of the execution form, `MerlinExecutionData`. */
export type MerlinExecutionValues = {
  id: number;
  name?: string | null;
  pdfFormat: boolean;
  inputVariables: Record<string, unknown>;
};

/** Answer of an execution: the document was downloaded, or the values were refused. */
export type MerlinExecuteResult =
  | { kind: "ok" }
  | { kind: "validationErrors"; validationErrors: ValidationError[] };

const NOT_ACCEPTABLE = 406;

/**
 * Creates the document and saves it (Word® or PDF). A refused value is HTTP 406 with the errors at
 * `inputVariables.<name>`.
 */
export async function executeMerlinTemplate(
  values: MerlinExecutionValues
): Promise<MerlinExecuteResult> {
  const res = await rawRequest("/rs/merlinexecution/execute", {
    method: "POST",
    body: JSON.stringify({ data: values }),
  });
  if (res.status === NOT_ACCEPTABLE) {
    const body = (await res.json().catch(() => null)) as {
      validationErrors?: ValidationError[];
    } | null;
    return {
      kind: "validationErrors",
      validationErrors: body?.validationErrors ?? [],
    };
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}`);
  }
  saveBlob(
    await responseBlob(res),
    parseContentDispositionFilename(res.headers.get("Content-Disposition"))
  );
  return { kind: "ok" };
}

/** The serial execution: the documents of all rows of the Excel® file, as a ZIP archive. */
export function executeMerlinSerial(id: number, file: File): Promise<void> {
  const body = new FormData();
  body.append("file", file);
  return downloadFile(`/rs/merlinexecution/serialExecution/${id}`, {
    method: "POST",
    body,
  });
}

/** The Excel® file for a serial execution, empty or prefilled with the employees or users. */
export function downloadMerlinSerialTemplate(
  id: number,
  fill: MerlinSerialFill
): Promise<void> {
  const query = fill === "base" ? "" : `?fill=${fill}`;
  return downloadFile(
    `/rs/merlinexecution/downloadSerialExecutionTemplate/${id}${query}`
  );
}
