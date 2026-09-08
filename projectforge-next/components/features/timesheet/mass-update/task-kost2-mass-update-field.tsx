"use client";

import { useEffect, useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import type { MassUpdateParameter } from "@/lib/rs/multi-select";
import { cn } from "@/lib/utils";
import { TaskKost2Picker } from "../task-kost2-picker";

/**
 * The task + cost unit control of the time sheet mass update — the mass-update adapter around the shared
 * [TaskKost2Picker], the counterpart of the edit form's TaskKost2Section.
 *
 * The picker is missing from the generic mass-update form (it is an entity picker with a dependency the
 * declared fields do not model), so it renders through the `customFields` slot at the position the backend
 * declares for `taskAndKost2` (below the activity report) and contributes its parameters under
 * the keys the backend expects: `task`/`kost2` carry the chosen ids and `taskAndKost2` is the synthetic
 * field the run gates the change on (`TimesheetMultiSelectedPageRest.checkParamHasAction`).
 *
 * The task control is *always* usable: a task must be pickable to load its cost units, and picking one
 * when no shared task is pre-filled is how a cost-unit-only change starts. The checkbox is a separate
 * opt-in for *writing* the task to every sheet — while it is off, a picked task only scopes the cost-unit
 * list and is not applied. Picking a task off the pre-filled one while the box stays off raises a prominent
 * orange warning (the task will not be changed), so the opt-out is never a silent surprise: a task applied
 * or skipped unnoticed was a recurring mistake in the legacy form. The *cost unit* needs no gate at all —
 * it is an explicit dropdown pick, so changing only the cost unit is a change on its own.
 *
 * The picker opens on what the selected sheets already share: [initialTaskId] is their deepest common task
 * and [initialKost2Id] the cost unit they all book on (computed server side, `initialParams`), so the cost
 * units reachable from that task are offered from the start. A parameter is posted only for what the user
 * actually acts on:
 * - the task is sent only when the opt-in is on and a task is selected; a task picked with the box off only
 *   scopes the cost units and posts nothing (otherwise the run would overwrite every sheet's own task);
 * - the cost unit is sent whenever one is selected, so a cost-unit-only, task-only or both change pins it;
 * - `taskAndKost2.change` gates the run and is set on any action — with nothing to do all three keys are
 *   dropped, so an untouched section acts on nothing.
 * The cost unit is validated against the task server side (`handleClientMassUpdateCall`), and a task change
 * maps the cost unit to the same type in the new project.
 */
export function TaskKost2MassUpdateField({
  setParam,
  initialTaskId,
  initialKost2Id,
}: {
  setParam: (name: string, param: MassUpdateParameter | undefined) => void;
  /** The task the selected sheets share, pre-selected so its cost units are offered from the start. */
  initialTaskId: number | null;
  /** The cost unit they all book on, pre-selected when it is reachable from [initialTaskId]. */
  initialKost2Id: number | null;
}) {
  const t = useTranslations();
  const checkboxId = useId();
  const [enabled, setEnabled] = useState(false);
  const [taskId, setTaskId] = useState<number | null>(() => initialTaskId);
  const [kost2Id, setKost2Id] = useState<number | null>(() => initialKost2Id);

  // The user picked a task off the pre-filled one — a change that only takes effect once the opt-in is on.
  const taskChanged = taskId !== initialTaskId;
  // A picked-but-not-opted-in task: warn that it will not be written, so the opt-out is not a silent one.
  const warnTaskIgnored = taskChanged && !enabled;

  useEffect(() => {
    // The task is written only with the opt-in on; the cost unit is a change on its own.
    const applyTask = enabled && taskId != null;
    const kost2Changed = kost2Id !== initialKost2Id;
    if (!applyTask && !kost2Changed) {
      // Nothing to do — post nothing, so a mount with a prefill (or a task picked only to scope the cost
      // units) acts on nothing.
      setParam("task", undefined);
      setParam("kost2", undefined);
      setParam("taskAndKost2", undefined);
      return;
    }
    // A task picked without the opt-in scopes the cost-unit list but is not posted; a selected cost unit
    // always is, so a cost-unit-only change pins it even while the task stays untouched.
    setParam("task", applyTask ? { id: taskId } : undefined);
    setParam("kost2", kost2Id != null ? { id: kost2Id } : undefined);
    setParam("taskAndKost2", { change: true });
    // `setParam` is stable (a useCallback in MassUpdateForm); the flag and ids drive the params.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, taskId, kost2Id, initialTaskId, initialKost2Id]);

  return (
    <div className="space-y-3">
      {/* Bordered and tinted so the opt-in is impossible to miss; turns to a warning tone once a task is
          picked without it, because the pick would otherwise be silently dropped (the legacy pitfall). */}
      <div
        className={cn(
          "rounded-md border p-3",
          warnTaskIgnored
            ? "border-warning bg-warning/10"
            : "border-primary/40 bg-primary/5"
        )}
      >
        <div className="flex items-center gap-2">
          <Checkbox
            id={checkboxId}
            checked={enabled}
            onCheckedChange={(value) => setEnabled(value === true)}
          />
          <Label htmlFor={checkboxId} className="cursor-pointer font-medium">
            {t("timesheet.massupdate.updateTask")}
          </Label>
        </div>
        {warnTaskIgnored && (
          <p className="mt-2 text-xs text-warning">
            {t("timesheet.massupdate.taskNotApplied")}
          </p>
        )}
      </div>
      <TaskKost2Picker
        taskId={taskId}
        kost2Id={kost2Id}
        onTaskChange={(ref) => setTaskId(ref?.id ?? null)}
        onKost2Change={setKost2Id}
      />
    </div>
  );
}
