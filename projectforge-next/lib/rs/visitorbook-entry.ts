import { rawRequest, RsError } from "./client";
import type { ResponseAction } from "./types";
import type { ValidityWriteResult } from "@/components/shared/validity-entries/types";
import type { VisitorbookEntry } from "@/components/features/visitorbook/types";

const NOT_ACCEPTABLE = 406;

/** Adds a visit to a visitor (`VisitorbookEntityRest.insertEntry`). */
export function insertVisit(entry: VisitorbookEntry) {
  return write("insert", entry);
}

/** Updates a visit; the backend keeps it at the visitor it is stored for. */
export function updateVisit(entry: VisitorbookEntry) {
  return write("update", entry);
}

/** Marks the visit as deleted; the backend keeps it for the history. */
export function deleteVisit(entry: VisitorbookEntry) {
  return write("delete", entry);
}

/**
 * Posts the visit in the `PostData` envelope. Answers with all visits of the visitor, the backend's validation
 * errors (406), or its refusal (a danger toast, e.g. no write access).
 */
async function write(
  action: "insert" | "update" | "delete",
  entry: VisitorbookEntry
): Promise<ValidityWriteResult<VisitorbookEntry>> {
  const path = `/rs/visitorbook/entry/${action}`;
  const data = {
    id: entry.id ?? null,
    dateOfVisit: entry.dateOfVisit ?? null,
    arrived: entry.arrived ?? null,
    departed: entry.departed ?? null,
    comment: entry.comment ?? null,
    visitorbookId: entry.visitorbookId,
  };
  const res = await rawRequest(path, {
    method: "POST",
    body: JSON.stringify({ data }),
  });
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
  const body = (await res.json().catch(() => null)) as
    | ({ entries?: VisitorbookEntry[] } & ResponseAction)
    | null;
  // `GlobalDefaultExceptionHandler` answers a `UserException` (an `AccessException` among them) with HTTP
  // 200 and a danger toast, see lib/rs/entity.ts.
  if (body?.targetType === "TOAST" && body.message?.color === "danger") {
    return {
      kind: "rejected",
      message:
        body.message.message ??
        body.message.technicalMessage ??
        body.message.i18nKey ??
        "",
    };
  }
  return { kind: "ok", entries: body?.entries ?? [] };
}
