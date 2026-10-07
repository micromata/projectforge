"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ComponentProps } from "react";
import {
  confirmLeaveUnsavedChanges,
  hasUnsavedChanges,
} from "@/hooks/use-unsaved-changes-warning";
import { navigateInGesture } from "@/lib/navigate-in-gesture";

/**
 * A link that asks before it throws away an edit form's unsaved changes.
 *
 * For every link that leaves an edit form: the breadcrumb back to the list, and the links out of a
 * form into another entity (an invoice position's order, an order position's invoices). Nothing is
 * asked when there is nothing to lose — see useUnsavedChangesWarning, which is where the form says so.
 *
 * `onNavigate` rather than `onClick`, so opening the link in a new tab (where the form stays put) is
 * not interrupted. The ask is the app's own dialog, which answers asynchronously (see
 * confirmLeaveUnsavedChanges): the navigation is held with `preventDefault` and, on "leave", replayed
 * with a `router.push`. Every GuardedLink target is a string url, so pushing it is exact.
 *
 * With nothing to lose it navigates the way GestureLink does, so that Safari keeps the entry.
 */
export function GuardedLink(props: ComponentProps<typeof Link>) {
  const router = useRouter();
  return (
    <Link
      {...props}
      onNavigate={(event) => {
        const href = props.href;
        // A url shape we can't replay: let the navigation go as it is.
        if (typeof href !== "string") {
          props.onNavigate?.(event);
          return;
        }
        if (!hasUnsavedChanges()) {
          navigateWithinGesture(router, props, event);
          return;
        }
        event.preventDefault();
        // After the dialog the click is long over, so there is no gesture left to push the entry in.
        void confirmLeaveUnsavedChanges().then((leave) => {
          if (leave) router.push(href);
        });
      }}
    />
  );
}

/**
 * A `next/link` whose history entry is created within the click (see navigateInGesture): without it,
 * Safari may skip the entry on back. For the in-app links that are no way out of an edit form (its own
 * tabs, the login pages) — every other in-app link is a GuardedLink, which navigates the same way.
 */
export function GestureLink(props: ComponentProps<typeof Link>) {
  const router = useRouter();
  return (
    <Link
      {...props}
      onNavigate={(event) => navigateWithinGesture(router, props, event)}
    />
  );
}

function navigateWithinGesture(
  router: ReturnType<typeof useRouter>,
  props: ComponentProps<typeof Link>,
  event: { preventDefault: () => void }
) {
  // The caller's own onNavigate may cancel the navigation.
  let cancelled = false;
  props.onNavigate?.({
    preventDefault: () => {
      cancelled = true;
      event.preventDefault();
    },
  });
  // A replacing link adds no entry, and an object url is left to Next.
  if (cancelled || props.replace || typeof props.href !== "string") return;
  event.preventDefault();
  navigateInGesture(router, props.href, { scroll: props.scroll ?? undefined });
}
