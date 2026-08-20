"use client";

import Link from "next/link";
import type { ComponentProps } from "react";
import { confirmLeaveUnsavedChanges } from "@/hooks/use-unsaved-changes-warning";

/**
 * A link that asks before it throws away an edit form's unsaved changes.
 *
 * For every link that leaves an edit form: the breadcrumb back to the list, and the links out of a
 * form into another entity (an invoice position's order, an order position's invoices). Nothing is
 * asked when there is nothing to lose — see useUnsavedChangesWarning, which is where the form says so.
 *
 * `onNavigate` rather than `onClick`, so opening the link in a new tab (where the form stays put) is
 * not interrupted.
 */
export function GuardedLink(props: ComponentProps<typeof Link>) {
  return (
    <Link
      {...props}
      onNavigate={(event) => {
        if (!confirmLeaveUnsavedChanges()) {
          event.preventDefault();
          return;
        }
        props.onNavigate?.(event);
      }}
    />
  );
}
