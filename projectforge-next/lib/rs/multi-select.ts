/**
 * The mass update of a list selection (`AbstractMultiSelectedPage`), layout free.
 *
 * Two REST bases are involved and they are not the same: the selection is *started* on the list's own
 * entity (`{entity}/startMultiSelection`), everything after that happens on the mass update page
 * (`{page}/meta`, `/select`, `/update`, `/download`, `/cancel`) — and its path is not derivable from
 * the entity's (`invoiceSelected` for `outgoingInvoice`), so a page declares it.
 *
 * The state lives in the HTTP session, keyed by the backend's `*Rest` class
 * (`MultiSelectionSupport.SessionContext`, 60 minutes): `startMultiSelection` registers every id the
 * filter matched, `select` narrows that to the ticked ones, and `meta`/`update` read them from there.
 * Nothing carries ids in a url, and a deployment behind a load balancer needs sticky sessions.
 *
 * The `dynamic`/`massUpdate`/`selected` endpoints of the same page answer the same information as a
 * `UILayout` and are deprecated; they are deliberately not wrapped here.
 */

import { rawRequest, request, RsError } from "./client";
import { downloadFile } from "./download";
import type { UIDataTypeName } from "@/lib/metadata/types";
import type {
  MagicFilter,
  ResultSet,
  UISelectValue,
  ValidationError,
} from "./types";

/** HTTP status Spring answers with when the update was rejected (see AbstractPagesRestUtils). */
const NOT_ACCEPTABLE = 406;

/**
 * The query parameter that asks a list to open in selection mode — the backend's
 * `MultiSelectionSupport.REQUEST_PARAM_MULTI_SELECTION`, and what `PagesResolver
 * .getMultiSelectionPageUrl` puts into every link that leads to a list *for* a mass update.
 *
 * Spelled the same here because those links are the backend's, not this app's: a page reached through
 * one must not open as a plain list the user has to switch over by hand.
 */
export const MULTI_SELECTION_PARAM = "multiSelectionMode";

/** One field the page may change — the backend's `MassUpdateFieldMeta`. */
export interface MassUpdateFieldMeta {
  /** Name of the field, and the key of its parameter in the posted map. */
  field: string;
  /**
   * Which property of the [MassUpdateParameter] the value goes into, e.g. `localDateValue`.
   *
   * Answered, never derived from `dataType` here: the mapping is the backend's, and a copy of it
   * would silently drop values the day a type is added.
   */
  valueProperty: string;
  label?: string;
  dataType?: UIDataTypeName;
  maxLength?: number;
  /** Rows of a textarea; absent for a single line input. */
  rows?: number;
  /** The options of an enum field, translated. */
  values?: UISelectValue[];
  /** Whether the field offers clearing the value on every selected entry. */
  deleteOption?: boolean;
  /** Whether it offers replacing a substring instead of the whole text. */
  replaceOption?: boolean;
  /** Whether it offers appending to the existing text. */
  appendOption?: boolean;
  /** Whether appending is the preset of those two. */
  appendPreset?: boolean;
}

/** Everything the mass update page needs — the backend's `MultiSelectMetaData`. */
export interface MultiSelectMeta {
  title: string;
  selectedCount: number;
  /** How many the list registered, i.e. how many the user may pick from. */
  registeredCount: number;
  fields: MassUpdateFieldMeta[];
  listPage: string;
  maxMassUpdate: number;
  /** An entity specific note above the fields, as markdown. */
  info?: string;
  /**
   * What the selected entries add up to, in the shape the entity's own list serves its statistics in
   * (`InvoiceStatistics` for the invoice) — rendered by the component the page declares, see
   * `MassUpdateDef.statisticsLine`.
   *
   * `unknown` because the shape is the entity's and nothing generic reads it. The deprecated
   * `statistics` field of the same response is that summary pre-rendered as markdown for the
   * `UILayout` form, and is not read here: its amounts are formatted server side and its colours are
   * raw HTML spans.
   */
  statisticsData?: unknown;
}

/**
 * One field's instruction: at most one of the four actions, plus the value it acts with.
 *
 * The value goes into the property `MassUpdateFieldMeta.valueProperty` names, which is why this is an
 * index type rather than a closed record — the frontend never decides which one that is. More than one
 * action is rejected by the backend (`massUpdate.error.invalidOptionMix`).
 */
export interface MassUpdateParameter {
  /** Clear the field on every selected entry. */
  delete?: boolean;
  /** Replace `replaceText` with the value, instead of overwriting the whole text. */
  change?: boolean;
  /** Append the value to the existing text. */
  append?: boolean;
  /** The text to look for when `change` is set. */
  replaceText?: string;
  textValue?: string;
  localDateValue?: string;
  timestampValue?: string;
  timeValue?: string;
  decimalValue?: string;
  /** An integer field's value — named after the property, which is a `longValue` holding an `Int`. */
  longValue?: number;
  booleanValue?: boolean;
  /** An entity the field points at (user, task, account …), by id. */
  id?: number;
}

/** What a run did — the backend's `MassUpdateResult`. */
export interface MassUpdateResult {
  modifiedCounter: number;
  unmodifiedCounter: number;
  errorCounter: number;
  /** The sentence summarizing the counters, translated by the backend. */
  resultMessage: string;
  errors: { identifier: string; message: string }[];
  /**
   * Where the Excel protocol of this run can be fetched; it expires after five minutes.
   *
   * A rest path without the `/rs/` prefix and without a leading slash (`invoiceSelected/download`,
   * `AbstractDynamicPageRest.getRestPath`), so it must not be fetched as it stands — see
   * [downloadMassUpdateProtocol].
   */
  downloadUrl?: string;
  /** The fields the update acted on, translated. */
  changedFields: string[];
}

/** Answer of a run: either it went through, or the server rejected it with something to fix. */
export type MassUpdateOutcome =
  | { kind: "ok"; result: MassUpdateResult }
  | { kind: "validationErrors"; validationErrors: ValidationError[] };

/** Which of the four actions a preview change describes — the backend's `MassUpdateAction`. */
export type MassUpdateAction =
  | "SET"
  | "APPEND"
  | "REPLACE"
  | "DELETE"
  | "DELETE_OCCURRENCES";

/** One field a mass update would act on — the backend's `MassUpdatePreviewChange`. */
export interface MassUpdatePreviewChange {
  field: string;
  label: string;
  action: MassUpdateAction;
  /** The value the action acts with (the searched text for `REPLACE`); absent for a plain delete. */
  value?: string;
  /** The replacement, for `REPLACE` only. */
  replaceValue?: string;
}

/** What a mass update would do, before it is committed — the backend's `MassUpdatePreview`. */
export interface MassUpdatePreview {
  selectedCount: number;
  changes: MassUpdatePreviewChange[];
}

/** Answer of a preview: what it would do, or the server rejected it with something to fix. */
export type MassUpdatePreviewOutcome =
  | { kind: "ok"; preview: MassUpdatePreview }
  | { kind: "validationErrors"; validationErrors: ValidationError[] };

/** Where the client goes next — the backend's `MultiSelectNavigation`. */
export interface MultiSelectNavigation {
  url: string;
  selectedCount: number;
}

/**
 * Registers every entry the given filter matches as selectable and answers how many those are.
 *
 * The filter, not a list of ids: the selection is meant to cover the whole result set, of which the
 * table only ever holds one page. The answer's url is the legacy mass update page, which a hand built
 * page ignores — it routes to its own.
 */
export function startMultiSelection(
  entity: string,
  filter: MagicFilter,
  signal?: AbortSignal
): Promise<MultiSelectNavigation> {
  return request<MultiSelectNavigation>(
    `/rs/${entity}/startSelection`,
    { method: "POST", body: JSON.stringify(filter) },
    signal
  );
}

/** Narrows the registered ids to the ones the user ticked. */
export function selectEntries(
  page: string,
  selectedIds: number[],
  signal?: AbortSignal
): Promise<MultiSelectNavigation> {
  return request<MultiSelectNavigation>(
    `/rs/${page}/select`,
    { method: "POST", body: JSON.stringify({ selectedIds }) },
    signal
  );
}

export function fetchMultiSelectMeta(
  page: string,
  signal?: AbortSignal
): Promise<MultiSelectMeta> {
  return request<MultiSelectMeta>(
    `/rs/${page}/meta`,
    { method: "GET" },
    signal
  );
}

/**
 * The selected entries themselves, in the same row shape the entity's list serves — so the page's own
 * column declarations render them (see SelectedEntriesPanel).
 *
 * Asked of the server rather than taken from the list's rows, because the selection is kept across a
 * change of the filter: an entry ticked under an earlier filter was never transferred to this client.
 */
export function fetchSelectedEntries<Row>(
  page: string,
  signal?: AbortSignal
): Promise<ResultSet<Row>> {
  return request<ResultSet<Row>>(
    `/rs/${page}/selectedList`,
    { method: "GET" },
    signal
  );
}

/**
 * Runs the update over the ids the session holds.
 *
 * A 406 is a regular answer here as everywhere else: nothing selected, nothing to do, or two actions on
 * one field are things the user fixes, not errors (see `showValidationErrors`).
 */
export async function massUpdate(
  page: string,
  params: Record<string, MassUpdateParameter>,
  signal?: AbortSignal
): Promise<MassUpdateOutcome> {
  const path = `/rs/${page}/update`;
  const res = await rawRequest(
    path,
    { method: "POST", body: JSON.stringify(params) },
    signal
  );
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
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return { kind: "ok", result: (await res.json()) as MassUpdateResult };
}

/**
 * Answers what the update would do, without writing anything — what the confirmation dialog lists.
 *
 * The server decides which fields have an action (the same check the run uses) and formats the values,
 * so the dialog shows what the backend understood, not what a client re-derived. A 406 is a regular
 * answer here as for [massUpdate]: two actions on one field or a missing replacement text are things
 * the user fixes, shown before the write rather than after.
 */
export async function previewMassUpdate(
  page: string,
  params: Record<string, MassUpdateParameter>,
  signal?: AbortSignal
): Promise<MassUpdatePreviewOutcome> {
  const path = `/rs/${page}/preview`;
  const res = await rawRequest(
    path,
    { method: "POST", body: JSON.stringify(params) },
    signal
  );
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
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }
  return { kind: "ok", preview: (await res.json()) as MassUpdatePreview };
}

/** Drops the selection and answers where the user came from. */
export function cancelMultiSelection(
  page: string,
  signal?: AbortSignal
): Promise<MultiSelectNavigation> {
  return request<MultiSelectNavigation>(
    `/rs/${page}/cancel`,
    { method: "GET" },
    signal
  );
}

/**
 * Saves the Excel protocol of the last run.
 *
 * Takes [MassUpdateResult.downloadUrl] as it comes and prefixes it here: the backend answers a rest path
 * relative to `/rs` (`invoiceSelected/download`), which a fetch would resolve against the *page's* url —
 * `/next/invoice/mass-update/invoiceSelected/download`, a 404. Normalizing at the one place that knows
 * what the value is, rather than in the panel that only passes it on.
 *
 * The download slot lives in the session and expires after five minutes, so an expired one answers 400 —
 * which [downloadFile] passes on as an [RsError] for the caller to show, instead of saving an empty file.
 */
export function downloadMassUpdateProtocol(
  url: string,
  signal?: AbortSignal
): Promise<void> {
  const path = url.startsWith("/rs/") ? url : `/rs/${url.replace(/^\/+/, "")}`;
  return downloadFile(path, { method: "GET" }, signal);
}
