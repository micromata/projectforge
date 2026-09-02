"use client";

import { useStore } from "@tanstack/react-form";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Fragment, type ReactNode } from "react";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import { fetchTaskInfo } from "@/lib/rs/task";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { TaskValues } from "../task-schema";
import { useKost2Preview } from "./use-kost2-preview";

const m = fromMetadata(TASK_METADATA);

/**
 * What the folded Finanzbuchhaltung card says — the values a reader would otherwise have to unfold it
 * for, one chip each (see FinanceSection for the fields themselves and SectionDef.collapsedSummary for
 * why a folded section shows a summary at all).
 *
 * The rule is "show only what deviates from the default", with two deliberate exceptions the effective
 * booking is read from: the resolved cost unit wild card and the booking status are always shown, since
 * "vererbt" and the project's cost prefix *are* the rule, not the absence of one. Everything else — a
 * black/white list, a protection date, the two flags — appears only once it is actually set.
 *
 * Reads live form values (like Kost2Block), so a value changed with the card open is already in the
 * summary when it is folded again.
 *
 * @param id null while the task is being added — then the project and its cost prefix are the parent's,
 *   which is what [useKost2Preview] and fetchTaskInfo resolve.
 */
export function FinanceSummary({ id }: { id: number | null }) {
  const t = useTranslations();
  const label = useFieldLabels(TASK_METADATA);
  const format = useFormatContext();
  const form = useEntityEditForm();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values as TaskValues
  );

  // The parent for a task that has no id yet — the only task there is to resolve a project from, as in
  // Kost2Block.
  const infoId = id ?? values.parentTask?.id ?? null;
  const { data: info } = useQuery({
    queryKey: ["taskInfo", infoId],
    queryFn: ({ signal }) => fetchTaskInfo(infoId!, signal),
    enabled: infoId != null,
    staleTime: Infinity,
  });
  const { preview } = useKost2Preview(id);

  const projektKost = preview?.projektKost ?? info?.projekt?.kost ?? null;
  const costConfigured = info?.costConfigured !== false;

  const bookingStatus = m
    .enumOptions("timesheetBookingStatus", t)
    .find((o) => o.value === values.timesheetBookingStatus);

  const chips: ReactNode[] = [];

  // Always, where cost units are used at all: the project's wild card is the effective set of bookable
  // cost units, with the resolved list as its tooltip — the same value and tooltip as the field above.
  if (costConfigured && projektKost) {
    chips.push(
      <HintTooltip plain text={preview?.kost2ListAsLines ?? " - (-)"}>
        <span className="font-mono">{`${projektKost}.*`}</span>
      </HintTooltip>
    );
  }
  // The restriction on that set, once one is typed — named as the field's own select does.
  if (values.kost2BlackWhiteList?.trim()) {
    chips.push(
      <Chip
        label={t(
          values.kost2IsBlackList
            ? "task.kost2list.blackList"
            : "task.kost2list.whiteList"
        )}
        value={values.kost2BlackWhiteList}
      />
    );
  }
  // Always: "vererbt" is the effective booking rule, not the lack of one.
  if (bookingStatus) {
    chips.push(
      <Chip
        label={label("timesheetBookingStatus")}
        value={bookingStatus.label}
      />
    );
  }
  if (values.protectTimesheetsUntil) {
    chips.push(
      <Chip
        label={label("protectTimesheetsUntil")}
        value={formatDate(values.protectTimesheetsUntil, format)}
      />
    );
  }
  if (values.protectionOfPrivacy) {
    chips.push(<span>{label("protectionOfPrivacy")}</span>);
  }
  if (values.allowTimeOverlap) {
    chips.push(<span>{label("allowTimeOverlap")}</span>);
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
