"use client";

import { useState } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Copy01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { RepeatableList } from "@/components/shared/form/repeatable-list";
import { useFieldArray } from "@/hooks/use-field-array";
import { fetchPredecessorEntries } from "@/lib/rs/hr-planning";
import { toast } from "@/lib/toast";
import { HRPlanningEntryRow } from "./entry-row";
import type {
  HRPlanningEntryValues,
  HRPlanningValues,
} from "./hr-planning-schema";
import {
  emptyEntryValues,
  isEmptyEntry,
  toEntryValues,
} from "./hr-planning-values";

/**
 * The entries of a planned week, one [HRPlanningEntryRow] each.
 *
 * A new week may take over the entries of the week before ("copy from predecessor", as the legacy form
 * offered it): the blank entries are replaced by copies of the previous week's, nothing is saved until
 * the user does.
 */
export function HRPlanningEntriesSection({ id }: { id: number | null }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const array = useFieldArray<HRPlanningEntryValues>("entries");
  const userId = useStore(
    form.store,
    (state: unknown) => (state as FormState).values.user?.id
  );
  const week = useStore(
    form.store,
    (state: unknown) => (state as FormState).values.week
  );
  const [copying, setCopying] = useState(false);

  const copyFromPredecessor = async () => {
    if (userId == null || !week) return;
    setCopying(true);
    try {
      const copies = await fetchPredecessorEntries(userId, week);
      if (copies.length === 0) {
        toast.info(t("datatable.no-records-found"));
        return;
      }
      form.setFieldValue(
        "entries",
        (previous: HRPlanningEntryValues[] | undefined) => [
          ...(previous ?? []).filter((entry) => !isEmptyEntry(entry)),
          ...copies.map(toEntryValues),
        ]
      );
    } catch (error) {
      toast.error(error instanceof Error ? error.message : String(error));
    } finally {
      setCopying(false);
    }
  };

  return (
    <div className="flex flex-col gap-3">
      {id == null && !readOnly && (
        <div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={copyFromPredecessor}
            disabled={copying || userId == null || !week}
          >
            <HugeiconsIcon icon={Copy01Icon} />
            {t("hr.planning.entry.copyFromPredecessor")}
          </Button>
        </div>
      )}
      <RepeatableList
        array={array}
        emptyText={t("hr.planning.notPlanned")}
        addLabel={readOnly ? undefined : t("hr.planning.tooltip.addEntry")}
        onAdd={readOnly ? undefined : () => array.add(emptyEntryValues())}
        row={(entry, index) => (
          <HRPlanningEntryRow
            entry={entry}
            prefix={array.fieldName(index, "")}
            number={index + 1}
            onRemove={readOnly ? undefined : () => array.remove(index)}
            onRestore={readOnly ? undefined : () => array.restore(index)}
          />
        )}
      />
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<HRPlanningValues, "user" | "week">;
}
