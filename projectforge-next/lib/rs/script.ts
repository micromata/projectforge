/**
 * The calls of the script pages besides the standard REST ones (`ScriptEntityRest` and
 * `MyScriptEntityRest`, list and form): the examples and the two source downloads of the administration,
 * and the execution (`ScriptExecuteRest` for the administration, `MyScriptExecuteRest` for the scripts a
 * user is allowed to execute) — the form, the execution itself, its log while it runs and the file it
 * produced.
 */

import type { Attachment } from "./attachments";
import { request } from "./client";
import { downloadFile } from "./download";

/** A user, group or task as the DTO references it: the id is what the backend stores. */
export type ScriptRef = {
  id: number;
  displayName?: string;
};

/** org.projectforge.business.scripting.ScriptParameterType. */
export type ScriptParameterType =
  | "INTEGER"
  | "DECIMAL"
  | "STRING"
  | "BOOLEAN"
  | "DATE"
  | "TIME_PERIOD"
  | "TASK"
  | "USER";

/**
 * `Script.Param`: a parameter's declaration (name, type, description) and, on the execution page, its
 * value — in the property its type uses. A time period is `dateValue` to `toDateValue`.
 */
export interface ScriptParam {
  name?: string | null;
  type?: ScriptParameterType | null;
  description?: string | null;
  stringValue?: string | null;
  intValue?: number | null;
  decimalValue?: number | null;
  booleanValue?: boolean | null;
  dateValue?: string | null;
  toDateValue?: string | null;
  userValue?: ScriptRef | null;
  taskValue?: ScriptRef | null;
}

/** The six parameter slots of a script, in their order. */
export const SCRIPT_PARAMETER_KEYS = [
  "parameter1",
  "parameter2",
  "parameter3",
  "parameter4",
  "parameter5",
  "parameter6",
] as const;

export type ScriptParameterKey = (typeof SCRIPT_PARAMETER_KEYS)[number];

/** `DownloadFileSupport.Download`: the file of the user's last execution, kept for a few minutes. */
export interface ScriptDownload {
  filename?: string | null;
  fileSize?: string | null;
  availableUntil?: string | null;
  filenameAndSize?: string | null;
}

/**
 * org.projectforge.rest.dto.Script. Every optional property is `?`: Spring leaves empty fields out of the
 * JSON (`JsonInclude.Include.NON_NULL`). On the execution page of `myscript` only name, type, description
 * and the parameters are sent (`AbstractScriptExecuteRest.createUserView`).
 */
export type Script = {
  id: number | null;
  name?: string | null;
  type?: "KOTLIN" | "GROOVY" | "INCLUDE" | null;
  description?: string | null;
  script?: string | null;
  filename?: string | null;
  availableVariables?: string | null;
  executableByGroups?: ScriptRef[] | null;
  executableByGroupsAsString?: string | null;
  executableByUsers?: ScriptRef[] | null;
  executableByUsersAsString?: string | null;
  /** The mail addresses of everybody allowed to execute the script (execution page of the admin). */
  executableByEmails?: string | null;
  executeAsUser?: ScriptRef | null;
  /** The pages showing a button for the script, see `ScriptPageTargets` (administration only). */
  pageTargetIds?: string[] | null;
  buttonLabel?: string | null;
  buttonTooltip?: string | null;
  /** Posted on execution only: the page the script was started from by its button. */
  pageTarget?: string | null;
  parameterNames?: string | null;
  /** The names of the embedded scripts. */
  includes?: string | null;
  attachments?: Attachment[] | null;
  attachmentsCounter?: number | null;
  attachmentsSize?: number | null;
  attachmentsSizeFormatted?: string | null;
  deleted?: boolean;
  created?: string | null;
  lastUpdate?: string | null;
} & Partial<Record<ScriptParameterKey, ScriptParam | null>>;

/** `ScriptEntityRest.Example`: one of the example scripts the ad-hoc editor may start with. */
export interface ScriptExample {
  index: number;
  title: string;
}

/** `AbstractScriptExecuteRest.LogEntry`, translated and formatted by the backend. */
export interface ScriptLogEntry {
  timestamp: string;
  level: "FATAL" | "ERROR" | "WARN" | "INFO" | "DEBUG" | "TRACE";
  levelAsString: string;
  message: string;
}

/** `AbstractScriptExecuteRest.ExecutionResult`: [result] is Markdown. */
export interface ScriptExecutionResult {
  result: string;
  hasErrors: boolean;
  download?: ScriptDownload | null;
}

/**
 * `ScriptPageTargets.PageTarget`: a page a script can show its button on, e.g. `list:order` (the order
 * book) or `orderStatistics:forecast`.
 */
export interface ScriptPageTarget {
  id: string;
  title: string;
  /** The route of the page in this app, e.g. `/order`. */
  route: string;
}

/** `MyScriptEntityRest.PageButton`: the button of a script on a page. */
export interface ScriptPageButton {
  id: number;
  label: string;
  tooltip?: string | null;
}

/** The answer of `load`: the script to execute and the file of the user's last execution. */
export interface ScriptExecuteForm {
  script: Script;
  download?: ScriptDownload | null;
  /** The page the script was started from by its button, whose current filter the script gets. */
  origin?: ScriptPageTarget | null;
  /** The log viewer of the scripting loggers (administration only). */
  logViewerUrl?: string | null;
}

/**
 * The two executions: the administration's (any script, or ad-hoc code) and the one of a user allowed to
 * execute a stored script.
 */
export type ScriptExecuteEndpoint = "scriptExecute" | "myScriptExecute";

export function fetchScriptExamples(
  signal?: AbortSignal
): Promise<ScriptExample[]> {
  return request<ScriptExample[]>(
    "/rs/script/examples",
    { method: "GET" },
    signal
  );
}

export function scriptExecuteQueryKey(
  endpoint: ScriptExecuteEndpoint,
  id: number | null,
  example: number | null,
  from: string | null
) {
  return ["script", "execute", endpoint, id, example, from] as const;
}

/**
 * The execution form. `id` null is ad-hoc code (administration only), optionally starting with the
 * example of the given index. `from` is the page target the script was started from by its button.
 */
export function loadScriptExecution(
  endpoint: ScriptExecuteEndpoint,
  id: number | null,
  example: number | null,
  from: string | null,
  signal?: AbortSignal
): Promise<ScriptExecuteForm> {
  const params = new URLSearchParams();
  if (id != null) params.set("id", String(id));
  if (example != null) params.set("example", String(example));
  if (from != null) params.set("from", from);
  return request<ScriptExecuteForm>(
    `/rs/${endpoint}/load?${params}`,
    { method: "GET" },
    signal
  );
}

/**
 * Executes the script. Of a stored script the backend only takes the parameter values; the code is the
 * stored one.
 */
export function executeScript(
  endpoint: ScriptExecuteEndpoint,
  script: Script
): Promise<ScriptExecutionResult> {
  return request<ScriptExecutionResult>(`/rs/${endpoint}/execute`, {
    method: "POST",
    body: JSON.stringify(script),
  });
}

/** The log of the user's running or last execution of the script (`id` null: ad-hoc code). */
export function fetchScriptLog(
  endpoint: ScriptExecuteEndpoint,
  id: number | null,
  signal?: AbortSignal
): Promise<ScriptLogEntry[]> {
  const query = id != null ? `?id=${id}` : "";
  return request<ScriptLogEntry[]>(
    `/rs/${endpoint}/refresh${query}`,
    { method: "GET" },
    signal
  );
}

/** The file the user's last execution produced. */
export function downloadScriptResult(
  endpoint: ScriptExecuteEndpoint
): Promise<void> {
  return downloadFile(`/rs/${endpoint}/download`);
}

/** The former versions of the script's code, as a ZIP. */
export function downloadScriptBackups(id: number): Promise<void> {
  return downloadFile(`/rs/script/downloadBackupScripts/${id}`);
}

/** The code as executed: the script with its includes resolved. */
export function downloadEffectiveScript(id: number): Promise<void> {
  return downloadFile(`/rs/script/downloadEffectiveScript/${id}`);
}

/** The pages a script can show its button on, for the edit form. */
export function fetchScriptPageTargets(
  signal?: AbortSignal
): Promise<ScriptPageTarget[]> {
  return request<ScriptPageTarget[]>(
    "/rs/script/pageTargets",
    { method: "GET" },
    signal
  );
}

export function scriptPageButtonsQueryKey(target: string) {
  return ["script", "pageButtons", target] as const;
}

/** The buttons of the scripts the user may execute on the page of `target`, mostly none. */
export function fetchScriptPageButtons(
  target: string,
  signal?: AbortSignal
): Promise<ScriptPageButton[]> {
  return request<ScriptPageButton[]>(
    `/rs/myscript/pageButtons?target=${encodeURIComponent(target)}`,
    { method: "GET" },
    signal
  );
}
