"use client";

import { Fragment } from "react";
import { formatDisplayName } from "@/lib/format";
import { inactiveRefClass, type InactiveFlags } from "@/lib/page-def/reference";
import { cn } from "@/lib/utils";
import { HighlightedText } from "@/components/shared/highlighted-text";

/** A referenced object as shown by name: anything with a display name and the inactive flags. */
export type NamedRef = InactiveFlags & { displayName?: string | null };

/** Whether a value is a list of referenced objects (users, groups, …), as a list cell may hold. */
export function isRefArray(value: unknown): value is NamedRef[] {
  return (
    Array.isArray(value) &&
    value.length > 0 &&
    value.every((item) => typeof item === "object" && item !== null)
  );
}

/**
 * One referenced object by its name, struck through when it is deleted or deactivated (see
 * inactiveRefClass) — the read-only counterpart of a chip, for detail views, tables and charts.
 */
export function RefName({
  value,
  highlight,
  className,
}: {
  value?: NamedRef | null;
  /** The search term to highlight in the name (see HighlightedText). */
  highlight?: string;
  className?: string;
}) {
  if (!value) return null;
  return (
    <span className={cn(inactiveRefClass(value), className)}>
      <HighlightedText text={formatDisplayName(value)} query={highlight} />
    </span>
  );
}

/**
 * Several referenced objects joined by ", ", each struck through on its own when it is no longer live —
 * a group's members, a user's groups, the users of an access list. Joining the names into one string
 * would lose who of them left.
 */
export function RefList({
  refs,
  highlight,
  className,
}: {
  refs?: readonly NamedRef[] | null;
  highlight?: string;
  className?: string;
}) {
  if (!refs?.length) return null;
  return (
    <span className={className}>
      {refs.map((ref, index) => (
        <Fragment key={index}>
          {index > 0 && ", "}
          <RefName value={ref} highlight={highlight} />
        </Fragment>
      ))}
    </span>
  );
}
