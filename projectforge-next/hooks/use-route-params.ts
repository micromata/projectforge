"use client";

import { usePathname } from "next/navigation";
import { matchRoute, type RouteParams } from "@/lib/route-params";

/**
 * The dynamic params of the current URL — the replacement for `useParams()` in this app.
 *
 * `useParams()` is unusable here: under `output: "export"` each dynamic route is prerendered exactly
 * once, from the placeholder its `generateStaticParams` returns, and the params baked into that
 * build are what the hook reports at runtime. `/next/book/25219084` would render the *new* book
 * form, because `book/[id]` was prerendered as `book/new`. `usePathname()` does follow the URL,
 * so the params are parsed from it against the route's own pattern.
 *
 * @param pattern the route pattern as it appears in `app/`, without the route group and basePath,
 *   e.g. `/book/[id]/history` or `/[category]/[type]/[...params]`.
 * @returns the params, or `null` while the pathname doesn't match the pattern (during the
 *   prerender pass, and for the instant a client-side navigation is still on the old URL).
 */
export function useRouteParams<T extends RouteParams = RouteParams>(
  pattern: string
): T | null {
  const pathname = usePathname();
  return matchRoute(pattern, pathname) as T | null;
}
