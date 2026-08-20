"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { leafKeyOf } from "@/lib/leaf-key";

export interface EditReturn {
  /** Where cancel, save, delete and the breadcrumb lead. */
  route: string;
  /** Label of the breadcrumb link, already translated. */
  label: string;
}

export interface UseEditReturnOptions {
  /** The routes a `?returnTo=` may name, first one being the default (see EditDef.returnTargets). */
  targets?: { route: string; labelKey: string }[];
  /** Where to go when the page declares no targets: the entity's own list. */
  fallback: { route: string; labelKey: string };
}

/**
 * Where an edit page returns to — the page the user came from, not a fixed list.
 *
 * A task is opened from the tree and (later) from its own list, and both expect to get the user back.
 * The caller says which by appending `?returnTo=`; this resolves it against the declared targets, so
 * only a route the page itself named can be reached. An unrecognized value falls back to the default
 * instead of being followed, which is why no url ever has to be sanitized here.
 *
 * The parameter needs no carrying along any more: the history and the analyses of an entry are tabs of
 * the edit route rather than routes of their own (see EditPageShell), so a detour through one of them
 * leaves the url — and with it the caller — where it was.
 *
 * `useSearchParams` reads nothing during the static export prerender, so a route using this must wrap
 * its client in `<Suspense>` — that is Next's requirement for a statically rendered page, and the
 * empty first read is harmless: without a parameter the default is exactly today's behaviour.
 */
export function useEditReturn({
  targets,
  fallback,
}: UseEditReturnOptions): EditReturn {
  const t = useTranslations();
  const requested = useSearchParams().get("returnTo");
  const target =
    targets?.find((entry) => entry.route === requested) ??
    targets?.[0] ??
    fallback;
  return {
    route: target.route,
    // Through leafKeyOf: a target's key may be a namespace as well (`task.title.list` is both the
    // list's title and the parent of `task.title.list.select`), and the bare key would throw.
    label: t(leafKeyOf(target.labelKey, t.has)),
  };
}
