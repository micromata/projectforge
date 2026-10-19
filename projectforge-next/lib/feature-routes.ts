import type { FeatureRoutes } from "@/lib/rs/types";

function segments(path: string): string[] {
  return path.split("/").filter((segment) => segment.length > 0);
}

function isPrefix(route: string[], path: string[]): boolean {
  return route.length <= path.length && route.every((segment, i) => segment === path[i]);
}

/**
 * Whether the page at `pathname` (without basePath, e.g. `/address/42/`) belongs to a switched off feature: the
 * longest of the routes being a prefix of it (segment by segment, `address` covers `address/42` but not
 * `addressBook`) decides, a disabled one blocks the page.
 *
 * The same rule as on the server (FeaturePathIndex), which answers such a page with 404 on loading it; this is for
 * navigating inside the app.
 */
export function isRouteDisabled(
  pathname: string,
  routes: FeatureRoutes | null | undefined
): boolean {
  if (!routes || routes.disabled.length === 0) return false;
  const path = segments(pathname);
  let best = -1;
  let disabled = false;
  const check = (route: string, isDisabled: boolean) => {
    const routeSegments = segments(route);
    if (routeSegments.length > best && isPrefix(routeSegments, path)) {
      best = routeSegments.length;
      disabled = isDisabled;
    }
  };
  routes.disabled.forEach((route) => check(route, true));
  routes.enabled.forEach((route) => check(route, false));
  return disabled;
}
