"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { RsError } from "@/lib/rs/client";
import { useListMeta } from "@/hooks/use-list-meta";

/** Spring's answer to a request the DAO refused (see GlobalDefaultExceptionHandler). */
const FORBIDDEN = 403;

/** Whether a thrown error is the backend refusing the read. */
export function isAccessDenied(error: unknown): boolean {
  return error instanceof RsError && error.status === FORBIDDEN;
}

export interface ReadAccessGuard {
  /** While this is true nothing is known yet, so a page must not decide anything. */
  isPending: boolean;
  /**
   * The user may not see this entity. The redirect is already under way; the caller renders nothing
   * (see below for why it can't wait for it).
   */
  denied: boolean;
}

/**
 * Keeps a page of an entity the user has no read access to from rendering at all.
 *
 * The counterpart of `AuthGuard` one level down: that one asks whether anybody is logged in, this one
 * whether *this* user may see *this* entity. Without it a user without the right got the complete page
 * — toolbar, columns, filters, exports — around an empty table, because the backend answered the list
 * call with a toast and no rows.
 *
 * Two sources, because either can be the first to know:
 * - `listMeta.userAccess.read`, which the backend fills from `BaseDao.hasLoggedInUserSelectAccess`
 *   (see `AbstractEntityRest.checkUserAccess`). Costs no request of its own — every list page reads
 *   the meta data anyway, and the query is cached per entity.
 * - a 403 from any read the page has already made, for the case that the right was withdrawn while
 *   the user sat on the page, or that a rest class overrides `getListMeta` without filling the flag.
 *   The two 403s that mean something else (a second factor, a stale CSRF token) never arrive here:
 *   `rawRequest` recovers them itself and only rethrows what it could not.
 *
 * Neither is the authorization — the DAO refuses the call either way. This only decides what the user
 * sees instead of a page they may not have.
 *
 * @param entity REST category, e.g. `outgoingInvoice`.
 * @param error An error of a read the page made itself, if it has one — the entry an edit page
 * fetched. Folded in here rather than watched separately so a page denied on both counts still says
 * so only once.
 */
export function useReadAccessGuard(
  entity: string,
  error?: unknown
): ReadAccessGuard {
  const meta = useListMeta(entity);
  const denied =
    meta.data?.userAccess?.read === false ||
    isAccessDenied(meta.error) ||
    isAccessDenied(error);
  useAccessDeniedRedirect(denied);
  // `denied` is returned rather than only acted upon, because the redirect happens in an effect and
  // therefore *after* the render that discovered it — the caller has to render nothing in that same
  // pass or the page it is being taken away from flashes up first.
  return { isPending: meta.isPending, denied };
}

/**
 * The reaction half of [useReadAccessGuard], for a page that learns of the denial from a read of its
 * own rather than from the meta data: the entity's own list call, once the meta data was already
 * fetched and said yes (a right withdrawn mid-session, or a rest class overriding `getListMeta`).
 *
 * Separate so the two levels of a list page can each watch their own read without both firing: the
 * shell's guard sees the meta data, the inner list its 403, and only the one that is actually denied
 * shows the message.
 */
export function useAccessDeniedRedirect(denied: boolean): void {
  const router = useRouter();
  const t = useTranslations();

  useEffect(() => {
    if (!denied) return;
    // A silent redirect would read as a broken link, so the reason is said out loud once.
    toast.error(t("access.exception.noAccess"));
    // replace, not push: a page the user may not have should not be one Back away.
    router.replace("/");
  }, [denied, router, t]);
}
