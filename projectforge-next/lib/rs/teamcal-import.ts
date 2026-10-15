/**
 * The ICS import of team events (`TeamEventImportRest`, mapped to `teamCalImport`). The upload, state,
 * reconcile, commit and cancel calls are the generic ones of ./import.ts bound to [ENTITY]; the only call of
 * its own is the choice of the target calendar, which decides what the events are reconciled against.
 */

import { rawRequest, RsError } from "./client";
import type { ImportView } from "@/components/shared/import/import-types";

/** REST path base of the ICS import — `TeamEventImportRest` is mapped here. */
export const ENTITY = "teamCalImport";

/**
 * Sets the calendar the stashed events go into and answers the view reconciled against it. A calendar the
 * user may not write into is a `400` carrying `{ error }`, whose (translated) message is thrown.
 */
export async function setTeamCalImportTarget(
  teamCalId: number,
  signal?: AbortSignal
): Promise<ImportView> {
  const path = `/rs/${ENTITY}/target?teamCalId=${teamCalId}`;
  const res = await rawRequest(path, { method: "POST" }, signal);
  const text = await res.text();
  if (!res.ok) {
    let error: string | undefined;
    try {
      error = (JSON.parse(text) as { error?: string }).error;
    } catch {
      // No JSON body: fall back to the status below.
    }
    throw new RsError(res.status, error ?? `${res.status}: ${path}`);
  }
  return JSON.parse(text) as ImportView;
}
