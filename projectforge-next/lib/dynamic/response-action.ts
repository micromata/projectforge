/**
 * Normalisation of `ResponseAction` (org.projectforge.ui.ResponseAction).
 *
 * A rest call of a dynamic page can answer in two shapes: either a bare `ResponseAction`, or a
 * `FormLayoutData` envelope that carries `ui`/`data` plus the very same action fields inline. Both
 * are folded into one [NormalizedAction] here so the interpreter only has to deal with one shape.
 */

import type {
  DynamicPageResponse,
  ResponseAction,
  TargetType,
} from "@/lib/rs/types";

/** Rest endpoint prefix of the Spring backend. Relative action urls are resolved against it. */
const REST_PREFIX = "/rs/";

/** An action plus the payload that came with it (`ui`/`data` of an UPDATE, for instance). */
export interface NormalizedAction {
  targetType?: TargetType;
  url?: string;
  merge?: boolean;
  message?: ResponseAction["message"];
  validationErrors?: ResponseAction["validationErrors"];
  variables?: Record<string, unknown>;
}

/**
 * Reads the action out of a response, no matter whether it arrived bare or inside a
 * `FormLayoutData` envelope.
 *
 * A `FormLayoutData` puts its payload in the top level `ui`/`data` fields while a bare
 * `ResponseAction` nests everything under `variables` - so the envelope's payload is lifted into
 * `variables` to give the interpreter a single place to look.
 */
export function normalizeAction(
  response: DynamicPageResponse & ResponseAction
): NormalizedAction {
  const variables: Record<string, unknown> = { ...(response.variables ?? {}) };
  if (response.ui !== undefined) variables.ui = response.ui;
  if (response.data !== undefined) variables.data = response.data;

  return {
    targetType: response.targetType,
    url: response.url,
    merge: response.merge,
    message: response.message,
    validationErrors: response.validationErrors,
    variables,
  };
}

/**
 * Resolves an action url onto a path this app can fetch.
 *
 * The backend hands out rest urls relative to its rest base (`book/save`), mirroring
 * `getServiceURL` of the legacy app; an url that already starts with a slash is origin-relative
 * and taken as is. Absolute urls (`https://…`) are left untouched - the caller opens those in a
 * new tab rather than fetching them.
 */
export function resolveRestUrl(url: string): string {
  if (/^[a-z][a-z0-9+.-]*:\/\//i.test(url)) return url;
  return url.startsWith("/") ? url : `${REST_PREFIX}${url}`;
}
