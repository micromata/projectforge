"use client";

import { useState, type ReactNode } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { CollapsibleSummary } from "@/components/shared/collapsible-summary";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { RepeatableRow } from "@/components/shared/form/repeatable-row";
import { useFormatContext } from "@/hooks/use-format";
import { compareText } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import type { CustomerSetValues } from "./types";

export interface CustomerSetRowProps {
  /** Name prefix of the row's fields, e.g. `groups[2].`. */
  prefix: string;
  set: CustomerSetValues;
  /** Names of a business unit's groups, listed ahead of its customers; none for a group. */
  groupNames?: string[];
  /** A business unit's tasks; listed by their own title, the last segment of the path. */
  tasks?: EntityRef[];
  /** Added in this edit: open, since it is there to be filled in. */
  isNew: boolean;
  onRemove: () => void;
  removeLabel: string;
  children: ReactNode;
}

/**
 * One customer group or business unit: folded, its header lists every member, alphabetically, so the
 * whole configuration is read without opening a row — as the positions of an order are.
 *
 * Folded rows keep their fields mounted, and a row holding an error opens by itself: the server's
 * errors (a customer in two groups) name a member field, which a folded row would otherwise hide.
 */
export function CustomerSetRow({
  prefix,
  set,
  groupNames = [],
  tasks = [],
  isNew,
  onRemove,
  removeLabel,
  children,
}: CustomerSetRowProps) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const format = useFormatContext();
  const [open, setOpen] = useState(isNew);
  const hasErrors = useStore(form.store, (s: unknown) =>
    Object.entries((s as FormState).fieldMeta ?? {}).some(
      ([name, meta]) =>
        name.startsWith(prefix) && (meta?.errors?.length ?? 0) > 0
    )
  );
  const sorted = (texts: string[]) =>
    [...texts].sort((a, b) => compareText(a, b, format));

  return (
    <RepeatableRow
      open={open || hasErrors}
      onOpenChange={setOpen}
      keepMounted
      tinted
      onRemove={onRemove}
      removeLabel={removeLabel}
      header={
        <CollapsibleSummary
          primary={
            <span
              className={cn(
                "min-w-0 flex-1 truncate font-medium",
                !set.name.trim() && "text-muted-foreground italic"
              )}
            >
              {set.name.trim() || t("fibu.customerGroups.name")}
            </span>
          }
          details={[
            ...sorted(groupNames).map((name) => (
              <Badge
                key={`g-${name}`}
                variant="secondary"
                className="font-normal"
              >
                {name}
              </Badge>
            )),
            ...sorted(
              tasks.map((task) => task.displayName.split(" | ").at(-1) ?? "")
            ).map((title) => (
              <Badge
                key={`k-${title}`}
                variant="outline"
                className="font-normal"
              >
                {title}
              </Badge>
            )),
            ...sorted(set.customers.map((c) => c.displayName)),
            // Italic, as the free-text customers are set apart in the checklists too.
            ...sorted(set.texts).map((text) => (
              <span key={`t-${text}`} className="italic">
                {text}
              </span>
            )),
          ]}
        />
      }
    >
      {children}
    </RepeatableRow>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  fieldMeta?: Record<string, { errors?: unknown[] } | undefined>;
}
