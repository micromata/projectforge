/**
 * Writing calls of an entity page (`AbstractPagesRest`): save, delete, undelete.
 *
 * These are the same endpoints the UILayout pages use, so they follow the same protocol and not
 * the plain-JSON shape of client.ts:
 *
 * - the body is a `PostData` envelope (`{ data }`), never the entity alone,
 * - the answer is a `ResponseAction` telling the client where to go, *not* the saved entity —
 *   the new id only arrives as `variables.id`, so the caller has to re-read the entity,
 * - HTTP 406 is a regular answer carrying `validationErrors` (see AbstractPagesRestUtils).
 *
 * The CSRF token is not passed here: `rawRequest` sets the header for every state changing call
 * (see RestCsrfProtection), and `SessionCsrfService.validateCsrfToken` reads it from there.
 */

import { rawRequest, RsError } from "./client";
import type { ResponseAction, ValidationError } from "./types";

/** Answer of a write: either it went through, or the server rejected the entity. */
export type EntityWriteResult =
  | {
      kind: "ok";
      /**
       * Id of the written entity, `null` if the backend didn't name one. Insert is the case that
       * needs it: the id doesn't exist before the save.
       */
      id: number | null;
      /** Where the backend wants the user to go next; hand built pages usually route themselves. */
      action: ResponseAction;
    }
  | { kind: "validationErrors"; validationErrors: ValidationError[] }
  | {
      kind: "rejected";
      /** Already translated by the backend, and the only thing it says about the refusal. */
      message: string;
    };

/** HTTP status Spring answers with when the entity failed validation. */
const NOT_ACCEPTABLE = 406;

/**
 * Inserts or updates an entity — one endpoint for both, told apart by `data.id` server-side.
 *
 * @param data The DTO as the backend's `*PagesRest` deserializes it, `id` unset for an insert.
 */
export function saveOrUpdateEntity<D extends object>(
  entity: string,
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  return write(`/rs/${entity}/saveorupdate`, "PUT", data, signal);
}

/**
 * Marks the entity as deleted, keeping it undeletable (`undeleteEntity`).
 *
 * This is the delete a historized entity supports, and the default one: it survives and
 * `RestPaths.UNDELETE` brings it back. The destroying counterpart is [forceDeleteEntity], offered only
 * by the few entities that allow it.
 */
export function markEntityAsDeleted<D extends object>(
  entity: string,
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  return write(`/rs/${entity}/markAsDeleted`, "DELETE", data, signal);
}

/**
 * Destroys the entity for good — the row *and* its change history, with no undo (`RestPaths.FORCE_DELETE`,
 * "Unwiderruflich löschen").
 *
 * Only the entities whose DAO sets `isForceDeletionSupport` offer it (a team event, an address); a
 * historized entity refuses it, which is why the default delete only marks (see markEntityAsDeleted).
 * The button is a per-page opt-in (`EditDef.forceDelete`), so this is reached only where the backend
 * accepts it, behind a confirmation (see EntityForceDeleteButton).
 */
export function forceDeleteEntity<D extends object>(
  entity: string,
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  return write(`/rs/${entity}/forceDelete`, "DELETE", data, signal);
}

/** Reverses [markEntityAsDeleted]. */
export function undeleteEntity<D extends object>(
  entity: string,
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  return write(`/rs/${entity}/undelete`, "PUT", data, signal);
}

/**
 * Tells the backend that editing was cancelled — nothing is written.
 *
 * Worth a call even though it changes no data: `onCancelEdit` runs the same `onAfterEdit` as a save
 * does (see AbstractEntityRest), which remembers the entry as the one edited last, so the list can
 * mark it. The answer is a plain redirect the hand built pages have no use for.
 */
export function cancelEntityEdit<D extends object>(
  entity: string,
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  return write(`/rs/${entity}/cancel`, "POST", data, signal);
}

/**
 * Action of an entity page that writes the entity as a side effect — `book/lendOut` and
 * `book/returnBook` (BookServicesRest) are the case: they change a few fields server-side and
 * then run through the very same `saveOrUpdate`, so the *whole* posted entity is persisted.
 *
 * Same protocol as [saveOrUpdateEntity] (PostData envelope, ResponseAction, 406), which is why
 * it lives here and not in list-actions.ts: those speak the plain JSON shape of client.ts.
 *
 * @param action Path segment after the entity, e.g. "lendOut".
 */
export function postEntityAction<D extends object>(
  entity: string,
  action: string,
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  return write(`/rs/${entity}/${action}`, "POST", data, signal);
}

/**
 * The prepared clone of an entity: the posted entry once more, but as a *new* one — no id anywhere,
 * and whatever the entity considers unrepeatable stripped (an invoice's number, its payment, see
 * `AbstractEntityRest.cloneData` and `OutgoingInvoiceEntityRest.prepareClone`).
 *
 * The one write-shaped call here that answers the entity instead of a `ResponseAction`, which is why
 * it doesn't go through [write]: nothing is saved, so there is no id to read and nowhere to go.
 *
 * The posted entity is *not* validated server-side — the clone is taken from the form as it stands,
 * errors and all, exactly as Wicket's `ignoreErrorOnClone` allows it.
 *
 * @throws RsError on 501 (the entity's REST class has no `cloneSupport`) and on 406 (no insert
 *   access) — both mean a button was offered that shouldn't have been.
 */
export async function cloneEntity<D extends object>(
  entity: string,
  data: D,
  signal?: AbortSignal
): Promise<D> {
  const res = await rawRequest(
    `/rs/${entity}/cloneData`,
    { method: "POST", body: JSON.stringify({ data }) },
    signal
  );
  if (!res.ok) {
    throw new RsError(
      res.status,
      `${res.status} ${res.statusText}: clone of ${entity}`
    );
  }
  return (await res.json()) as D;
}

/**
 * Clones the entity and saves the copy in one call — the `CloneSupport.AUTOSAVE` counterpart of
 * [cloneEntity] (`RestPaths.CLONE`, not `cloneData`). The backend runs `prepareClone` (ids dropped)
 * and then the very same `saveOrUpdate` a normal save does, so the clone is validated and persisted.
 *
 * Told apart from a clone that fell back to editing by the answer's `targetType`, because both come
 * back as HTTP 200: a save answers with its `REDIRECT` (`onAfterEdit`), while `AbstractPagesRest.clone`
 * discards a failed save and re-serves the form as an `UPDATE`. So the caller reads a successful save
 * as `kind: "ok"` with `action.targetType !== "UPDATE"`, and the `UPDATE` — an overlapping time period
 * is the case — as "not saved, left on the form" (see runClone). Same `write` protocol otherwise.
 */
export function cloneAndSaveEntity<D extends object>(
  entity: string,
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  return write(`/rs/${entity}/clone`, "POST", data, signal);
}

/**
 * Turns this entity into a *different* one — a time sheet into a calendar event and back
 * (`TimesheetPagesRest.switch2CalendarEvent`, `TeamEventPagesRest.switch2Timesheet`).
 *
 * A sibling of [cloneEntity]: nothing is saved either, the posted form travels unvalidated, and the
 * answer is the prepared new entry. It differs only in what comes back — the target entity, not this
 * one — and in the envelope: these endpoints are layout endpoints and answer a `ResponseAction`, so the
 * prepared entry rides under `variables.data` (the same slot the attachment and save calls read).
 *
 * @param action the switch endpoint on *this* entity's REST class (e.g. `switch2CalendarEvent`).
 * @returns the target entity as the backend prepared it, to hand to its add page (see usePendingClone).
 * @throws RsError when the endpoint answers anything but 200 — a button offered where it shouldn't be.
 */
export async function convertEntity<Target extends object>(
  entity: string,
  action: string,
  data: object,
  signal?: AbortSignal
): Promise<Target> {
  const res = await rawRequest(
    `/rs/${entity}/${action}`,
    { method: "POST", body: JSON.stringify({ data }) },
    signal
  );
  if (!res.ok) {
    throw new RsError(
      res.status,
      `${res.status} ${res.statusText}: convert of ${entity} via ${action}`
    );
  }
  const body = (await res.json().catch(() => null)) as ResponseAction | null;
  const prepared = body?.variables?.data;
  if (!prepared || typeof prepared !== "object") {
    throw new RsError(res.status, `convert of ${entity}: no data in response`);
  }
  return prepared as Target;
}

async function write<D extends object>(
  path: string,
  method: "PUT" | "POST" | "DELETE",
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  const res = await rawRequest(
    path,
    { method, body: JSON.stringify({ data }) },
    signal
  );

  if (res.status === NOT_ACCEPTABLE) {
    const body = (await res.json().catch(() => null)) as ResponseAction | null;
    return {
      kind: "validationErrors",
      validationErrors: body?.validationErrors ?? [],
    };
  }
  if (!res.ok) {
    throw new RsError(res.status, `${res.status} ${res.statusText}: ${path}`);
  }

  const action = (await res.json().catch(() => null)) as ResponseAction | null;
  // A stale CSRF token is answered with HTTP 200 and an UPDATE action carrying the error - the
  // shape the UILayout pages read. rawRequest already retried with a fresh token, so seeing it
  // here means the retry failed too, and swallowing it would look like a successful save.
  if (action?.validationErrors?.length) {
    return {
      kind: "validationErrors",
      validationErrors: action.validationErrors,
    };
  }
  // A refusal the backend reports as an exception that escaped the controller: `GlobalDefaultExceptionHandler`
  // answers a `UserException` with `displayUserMessage` with HTTP *200* and nothing but a danger toast
  // (`UIToast.createExceptionToast`), so without this branch such an answer would read as a successful
  // write and the form would reset and navigate away although nothing was written.
  //
  // Not the path of the four CRUD endpoints above: there `AbstractPagesRestUtils` catches everything
  // around the DAO call and `handleException` turns a `UserException` — an `AccessException` among them
  // — into HTTP 406 with a `validationErrors` entry (fieldless, as `AccessException` sets no
  // `causedByField`), which the branch above already handles. What is left for this one are the custom
  // endpoints reached through [postEntityAction], which have no such catch.
  //
  // Told apart from a successful save with an extra warning (the order's notification mail that could
  // not be sent) by the target type: a refusal is a TOAST and nothing else, while a save answers with
  // the REDIRECT its message rides along on.
  if (action?.targetType === "TOAST" && action.message?.color === "danger") {
    return {
      kind: "rejected",
      message:
        action.message.message ??
        action.message.technicalMessage ??
        action.message.i18nKey ??
        "",
    };
  }
  return { kind: "ok", id: readId(action), action: action ?? {} };
}

/**
 * Reads the id out of `ResponseAction.variables` (`onAfterEdit` puts it there).
 *
 * `-1` is the backend's stand-in for "no id" and means the same as a missing value here.
 */
function readId(action: ResponseAction | null): number | null {
  const raw = action?.variables?.id;
  const id = typeof raw === "string" ? Number(raw) : raw;
  return typeof id === "number" && Number.isFinite(id) && id > 0 ? id : null;
}
