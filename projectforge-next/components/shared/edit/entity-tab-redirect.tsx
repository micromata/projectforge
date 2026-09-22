"use client";

import { useEffect } from "react";
import { notFound, useRouter } from "next/navigation";
import { TAB_PARAM } from "@/components/shared/edit-page-tabs";
import { useRouteParams } from "@/hooks/use-route-params";

export interface EntityTabRedirectProps {
  /** Route pattern of the old page as it appears in `app/`, e.g. `/book/[id]/history`. */
  pattern: string;
  /** Route of the list, e.g. `/book` — the edit page hangs off it. */
  route: string;
  /** Tab the old page has become, e.g. `history`. */
  tab: string;
}

/**
 * Sends an old deep link to the tab that replaced it: `/book/25/history` → `/book/25?tab=history`.
 *
 * History and forecast used to be routes of their own, which is what caused the bug they were split
 * off for: leaving the form unmounted it, and a half-filled form was gone on the way back. They are
 * tabs of the edit route now (see EditPageShell), but the old URLs are in bookmarks and in mails, so
 * they keep working.
 *
 * `replace`, not `push`: the old URL is not a place to go back to.
 */
export function EntityTabRedirect({
  pattern,
  route,
  tab,
}: EntityTabRedirectProps) {
  const router = useRouter();
  const raw = useRouteParams<{ id: string }>(pattern)?.id;
  const id = raw === undefined ? undefined : Number(raw);
  const target =
    id !== undefined && Number.isFinite(id) && id > 0
      ? `${route}/${id}?${TAB_PARAM}=${tab}`
      : null;

  useEffect(() => {
    if (target) router.replace(target);
  }, [router, target]);

  // An entry that isn't saved yet ("new") has neither a history nor an analysis — and the placeholder
  // the static export prerenders is exactly that, so this is also the branch the build takes.
  if (raw !== undefined && !target) notFound();
  return null;
}
