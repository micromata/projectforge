/**
 * Matching a Next.js route pattern against a concrete pathname.
 *
 * Needed because the app is a static export: every dynamic route is prerendered once from the
 * placeholder of its `generateStaticParams`, and `useParams()` hands back exactly that placeholder
 * (`"new"`) no matter which URL the browser is on. See use-route-params.ts for the hook and
 * scripts/generate-spa-shell-map.mjs for the server side of the same problem.
 */

/** A route pattern segment: `books`, `[id]` or `[...params]`. */
type Segment =
  | { kind: "literal"; value: string }
  | { kind: "param"; name: string }
  | { kind: "catchAll"; name: string };

function parsePattern(pattern: string): Segment[] {
  return splitPath(pattern).map((segment) => {
    const inner = segment.startsWith("[") && segment.endsWith("]");
    if (!inner) return { kind: "literal", value: segment };
    const name = segment.slice(1, -1);
    return name.startsWith("...")
      ? { kind: "catchAll", name: name.slice(3) }
      : { kind: "param", name };
  });
}

function splitPath(path: string): string[] {
  return path.split("/").filter(Boolean);
}

/** What a matched route yields: a single value per `[param]`, a list per `[...catchAll]`. */
export type RouteParams = Record<string, string | string[]>;

/**
 * The params of `pathname` under `pattern`, or `null` if it doesn't match.
 *
 * `pathname` is the app-relative path as `usePathname()` reports it, i.e. without the `basePath`.
 * Segments are decoded, matching what Next's own params contain.
 */
export function matchRoute(
  pattern: string,
  pathname: string
): RouteParams | null {
  const segments = parsePattern(pattern);
  const parts = splitPath(pathname);
  const params: RouteParams = {};

  for (let i = 0; i < segments.length; i++) {
    const segment = segments[i];
    if (segment.kind === "catchAll") {
      // Greedy and therefore last: it takes every remaining part, and needs at least one.
      const rest = parts.slice(i);
      if (rest.length === 0) return null;
      params[segment.name] = rest.map(decode);
      return params;
    }
    const part = parts[i];
    if (part === undefined) return null;
    if (segment.kind === "literal") {
      if (part !== segment.value) return null;
    } else {
      params[segment.name] = decode(part);
    }
  }
  // A trailing part with no segment left to consume it is a different (deeper) route.
  return parts.length === segments.length ? params : null;
}

function decode(part: string): string {
  try {
    return decodeURIComponent(part);
  } catch {
    // A malformed escape is not ours to repair — the caller validates the value anyway.
    return part;
  }
}
