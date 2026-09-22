"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ComponentProps } from "react";
import {
  confirmLeaveUnsavedChanges,
  hasUnsavedChanges,
} from "@/hooks/use-unsaved-changes-warning";

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
 */
export function GuardedLink(props: ComponentProps<typeof Link>) {
  const router = useRouter();
  return (
    <Link
      {...props}
      onNavigate={(event) => {
        const href = props.href;
        // Nothing to lose (or a url shape we can't replay): let the navigation go as it is.
        if (!hasUnsavedChanges() || typeof href !== "string") {
          props.onNavigate?.(event);
          return;
        }
        event.preventDefault();
        void confirmLeaveUnsavedChanges().then((leave) => {
          if (leave) router.push(href);
        });
      }}
    />
  );
}
