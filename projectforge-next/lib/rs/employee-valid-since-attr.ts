import { rawRequest, RsError } from "./client";
import type { ResponseAction } from "./types";
import type { ValidityWriteResult } from "@/components/shared/validity-entries/types";
import type { EmployeeValidSinceAttr } from "@/components/features/employee/types";

const NOT_ACCEPTABLE = 406;

/** Inserts a time-dependent attribute of an employee (`EmployeeEntityRest.insertValidSinceAttr`). */
export function insertValidSinceAttr(attr: EmployeeValidSinceAttr) {
  return write("insert", attr);
}

export function updateValidSinceAttr(attr: EmployeeValidSinceAttr) {
  return write("update", attr);
}

/** Marks the attribute as deleted; the backend keeps it for the history. */
export function deleteValidSinceAttr(attr: EmployeeValidSinceAttr) {
  return write("delete", attr);
}

/**
 * Posts the attribute in the `PostData` envelope. The value travels as a string for every type (the
 * column is a string); the dialog holds a number as a number. Answers with all entries of the attribute's
 * type, the backend's validation errors (406), or its refusal (a danger toast, e.g. no write access).
 */
async function write(
  action: "insert" | "update" | "delete",
  attr: EmployeeValidSinceAttr
): Promise<ValidityWriteResult<EmployeeValidSinceAttr>> {
  const path = `/rs/employee/validSinceAttr/${action}`;
  const data = {
    id: attr.id ?? null,
    validSince: attr.validSince ?? null,
    value: attr.value == null || attr.value === "" ? null : String(attr.value),
    comment: attr.comment ?? null,
    type: attr.type,
    employeeId: attr.employeeId,
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
    | ({ entries?: EmployeeValidSinceAttr[] } & ResponseAction)
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
