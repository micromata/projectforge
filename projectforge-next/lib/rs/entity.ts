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
 * This is the delete a historized entity supports; `RestPaths.DELETE`/`FORCE_DELETE` destroy the
 * row and its history and are deliberately not wired up here.
 */
export function markEntityAsDeleted<D extends object>(
  entity: string,
  data: D,
  signal?: AbortSignal
): Promise<EntityWriteResult> {
  return write(`/rs/${entity}/markAsDeleted`, "DELETE", data, signal);
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
