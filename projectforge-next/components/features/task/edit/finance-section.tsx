"use client";

import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { useNewEntryParams } from "@/hooks/use-new-entry-params";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { Kost2Block } from "./kost2-block";
import { TASK_NEW_ENTRY_PARAMS, type TaskDetail } from "../types";

const m = fromMetadata(TASK_METADATA);

/**
 * The finance administration of a task: the cost unit block and the four fields only the accounting
 * staff — and, for the cost units, the manager of the project — may change.
 *
 * Hand-rendered rather than declared because writability is not a property of the field but of *this*
 * user's access to *this* task: `TaskDao.hasAccessForKost2AndTimesheetBookingStatus` and membership of
 * the finance group, which the DTO reports as two flags (see Task.kt). A declaration says what is
 * shown, never who may change it.
 *
 * The divergence from Wicket is deliberate and the same one the order's "fully invoiced" flag
 * established: a field the user may not change is *shown and disabled* with the backend's own refusal
 * message as its explanation, instead of being silently uneditable. The authority remains the DAO —
 * only it knows whether a value actually changed, and it refuses with exactly these messages.
 *
 * @param id null while the task is being added. The rights are then the *parent's* — Wicket asks the same
 *   question with the parent node — and the preset answers them, so nothing here has to assume.
 */
export function FinanceSection({ id }: { id: number | null }) {
  const t = useTranslations();
  const label = useFieldLabels(TASK_METADATA);
  // A cache read of the task the form was filled from, not a second request: the access flags are the
  // server's and are not part of the form's values. The parameter names have to be the page's own, or
  // a new task's preset would be read under a second key and fetched again (see useEntityDetail).
  const task = useEntityDetail<TaskDetail>(
    "task",
    id,
    useNewEntryParams(TASK_NEW_ENTRY_PARAMS)
  ).data;
  // Read for a new task as well, where they are the rights on the parent it is added below (see
  // TaskPagesRest.newBaseDTO): a subtask of a task somebody else's project owns is not writable here
  // just because it has no id yet. Locked until the answer is in — the fields are enabled by a right,
  // not by a pending request.
  const kost2Access = task?.kost2AndBookingStatusWriteAccess === true;
  const protectAccess = task?.protectTimesheetsUntilWriteAccess === true;

  return (
    <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
      <Kost2Block id={id} writeAccess={kost2Access} className="md:col-span-3" />
      <SelectField
        name="timesheetBookingStatus"
        label={label("timesheetBookingStatus")}
        options={m.enumOptions("timesheetBookingStatus", t)}
        disabled={!kost2Access}
        hint={
          kost2Access
            ? undefined
            : t("task.error.timesheetBookingStatus2Readonly")
        }
      />
      <InputField
        name="protectTimesheetsUntil"
        type="date"
        label={label("protectTimesheetsUntil")}
        disabled={!protectAccess}
        hint={
          protectAccess
            ? undefined
            : t("task.error.protectTimesheetsUntilReadonly")
        }
      />
      <CheckboxField
        name="protectionOfPrivacy"
        label={label("protectionOfPrivacy")}
        disabled={!protectAccess}
        // What the flag does, or — where it may not be set — why not. Both are sentences the user
        // needs; the refusal is the more urgent one.
        hint={
          protectAccess
            ? t("task.protectionOfPrivacy.tooltip")
            : t("task.error.protectionOfPrivacyReadonly")
        }
      />
      {/* Not access-gated: a structural property of the task (inherited by its subtree), editable by
          whoever may edit the task at all — unlike the four fields above, which the DAO reserves for
          the finance group or the project's manager. */}
      <CheckboxField
        name="allowTimeOverlap"
        label={label("allowTimeOverlap")}
        hint={t("task.allowTimeOverlap.tooltip")}
      />
    </div>
  );
}
