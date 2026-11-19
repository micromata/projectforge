"use client";

import { useCallback, useRef } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { navigateInGesture } from "@/lib/navigate-in-gesture";
import { hrefWithParams } from "@/lib/url-param";

/**
 * A dialog whose state is query params of the page it opens over, so a copied url or a bookmark shows it again
 * and Back closes it (the React app routed its modals). [keys] are all the params the dialog owns: `open` sets
 * them, `close` removes them, the other params of the page stay. `params` is what the url says now.
 *
 * Opening pushes a history entry in the click that asked for it (see navigateInGesture). Closing goes back if
 * this hook pushed that entry, else (a reload, a shared link) replaces the url by the one without the params, so
 * Back doesn't lead into a dialog that was just closed.
 *
 * `useSearchParams` is covered by the authenticated layout's Suspense boundary under `output: export`.
 */
export function useUrlDialog(keys: readonly string[]) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const pushed = useRef(false);
  // A stable dependency for the callbacks, whatever array the caller passes.
  const keyList = keys.join("\n");
  const open = useCallback(
    (values: Record<string, string>) => {
      pushed.current = true;
      navigateInGesture(router, hrefWithParams(pathname, searchParams, values));
    },
    [router, pathname, searchParams]
  );
  const close = useCallback(() => {
    const removed = Object.fromEntries(
      keyList.split("\n").map((key) => [key, null])
    );
    if (pushed.current) {
      pushed.current = false;
      router.back();
    } else {
      router.replace(hrefWithParams(pathname, searchParams, removed));
    }
  }, [router, pathname, searchParams, keyList]);
  return { params: searchParams, open, close };
}
