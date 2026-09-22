"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Fragment, type ReactNode } from "react";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate, formatNumber, formatPercentage } from "@/lib/format";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { TaskValues } from "../task-schema";

const m = fromMetadata(TASK_METADATA);

/**
 * What the folded Gantt card says — the values a reader would otherwise have to unfold it for, one chip
 * each (see the `gantt` section in task.page.tsx for the fields themselves and
 * SectionDef.collapsedSummary for why a folded section shows a summary at all).
 *
 * Unlike FinanceSummary there is no effective-default to always surface here: a Gantt setting that is
 * unset simply isn't a constraint, so a chip appears only once its field actually holds a value. The
 * order of the chips is the order of the fields in the card.
 *
 * Reads live form values (like FinanceSummary), so a value changed with the card open is already in the
 * summary when it is folded again.
 */
export function GanttSummary() {
  const t = useTranslations();
  const label = useFieldLabels(TASK_METADATA);
  const format = useFormatContext();
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values as TaskValues
  );

  const objectType = m
    .enumOptions("ganttObjectType", t)
    .find((o) => o.value === values.ganttObjectType);
  const relationType = m
    .enumOptions("ganttRelationType", t)
    .find((o) => o.value === values.ganttRelationType);

  const chips: ReactNode[] = [];

  if (objectType) {
    chips.push(
      <Chip label={label("ganttObjectType")} value={objectType.label} />
    );
  }
  if (values.startDate) {
    chips.push(
      <Chip
        label={label("startDate")}
        value={formatDate(values.startDate, format)}
      />
    );
  }
  if (values.endDate) {
    chips.push(
      <Chip
        label={label("endDate")}
        value={formatDate(values.endDate, format)}
      />
    );
  }
  if (values.progress != null) {
    chips.push(
      <Chip
        label={label("progress")}
        value={formatPercentage(values.progress, format)}
      />
    );
  }
  if (values.duration != null) {
    chips.push(
      <Chip
        label={label("duration")}
        value={`${formatNumber(values.duration, format)} ${t("days")}`}
      />
    );
  }
  if (values.ganttPredecessorOffset != null) {
    chips.push(
      <Chip
        label={label("ganttPredecessorOffset")}
        value={`${formatNumber(values.ganttPredecessorOffset, format)} ${t("days")}`}
      />
    );
  }
  if (relationType) {
    chips.push(
      <Chip label={label("ganttRelationType")} value={relationType.label} />
    );
  }
  if (values.ganttPredecessor?.displayName) {
    chips.push(
      <Chip
        label={label("ganttPredecessor")}
        value={values.ganttPredecessor.displayName}
      />
    );
  }

  return (
    <>
      {chips.map((chip, index) => (
        <Fragment key={index}>{chip}</Fragment>
      ))}
    </>
  );
}

/** One `label: value` chip, the label muted and the value in the foreground so the pair reads apart. */
function Chip({ label, value }: { label: string; value: string }) {
  return (
    <span>
      {label}: <span className="text-foreground">{value}</span>
    </span>
  );
}
