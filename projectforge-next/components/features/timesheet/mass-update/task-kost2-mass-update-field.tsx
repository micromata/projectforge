"use client";

import { useEffect, useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import type { MassUpdateParameter } from "@/lib/rs/multi-select";
import { TaskKost2Picker } from "../task-kost2-picker";

/**
 * The task + cost unit control of the time sheet mass update — the mass-update adapter around the shared
 * [TaskKost2Picker], the counterpart of the edit form's TaskKost2Section.
 *
 * The picker is missing from the generic mass-update form (it is an entity picker with a dependency the
 * declared fields do not model), so it rides the `extraFields` slot and contributes its parameters under
 * the keys the backend expects: `task`/`kost2` carry the chosen ids and `taskAndKost2` is the synthetic
 * field the run gates the change on (`TimesheetMultiSelectedPageRest.checkParamHasAction`).
 *
 * Changing the *task* is an explicit opt-in: the checkbox unlocks the task control, and while it is off the
 * task stays at the shared value. This is deliberate and prominent — a task silently applied because nobody
 * noticed it was pre-filled was a recurring mistake in the legacy form. The *cost unit* needs no such gate:
 * it is an explicit dropdown pick, so it stays selectable on its own — changing only the cost unit (for the
 * shared task) is the common case and must not require touching the task.
 *
 * The picker opens on what the selected sheets already share: [initialTaskId] is their deepest common task
 * and [initialKost2Id] the cost unit they all book on (computed server side, `initialParams`), so the cost
 * units reachable from that task are offered from the start. A parameter is posted only for what the user
 * actually moved off that preset:
 * - the task is sent only when it changed (only possible with the opt-in on), because posting the unchanged
 *   shared task would make the run overwrite every sheet's own (deeper) task with the common ancestor;
 * - the cost unit is sent whenever one is selected, so a cost-unit-only, task-only or both change pins it;
 * - `taskAndKost2.change` gates the run and is set on any change — with nothing changed all three keys are
 *   dropped, so an untouched section acts on nothing.
 * The cost unit is validated against the task server side (`handleClientMassUpdateCall`), and a task-only
 * change maps the cost unit to the same type in the new project.
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

  useEffect(() => {
    // The task only counts when the opt-in is on; the cost unit counts on its own.
    const taskChanged = enabled && taskId != null && taskId !== initialTaskId;
    const kost2Changed = kost2Id !== initialKost2Id;
    if (!taskChanged && !kost2Changed) {
      // Nothing off the preset — post nothing, so the mount with a prefill acts on nothing.
      setParam("task", undefined);
      setParam("kost2", undefined);
      setParam("taskAndKost2", undefined);
      return;
    }
    // The unchanged shared task must not be posted (the run would set every sheet to the common ancestor);
    // a selected cost unit always is, so keeping the preset while changing the task still pins it.
    setParam("task", taskChanged ? { id: taskId } : undefined);
    setParam("kost2", kost2Id != null ? { id: kost2Id } : undefined);
    setParam("taskAndKost2", { change: true });
    // `setParam` is stable (a useCallback in MassUpdateForm); the flag and ids drive the params.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, taskId, kost2Id, initialTaskId, initialKost2Id]);

  return (
    <div className="space-y-3">
      {/* Bordered and tinted so the opt-in is impossible to miss — the legacy checkbox was overlooked. */}
      <div className="flex items-center gap-2 rounded-md border border-primary/40 bg-primary/5 p-3">
        <Checkbox
          id={checkboxId}
          checked={enabled}
          // Withdrawing the opt-in reverts any task pick, so the section falls back to the shared task and
          // the breadcrumb never shows an edit the run would ignore. A cost unit change survives on its own.
          onCheckedChange={(value) => {
            const next = value === true;
            setEnabled(next);
            if (!next) setTaskId(initialTaskId);
          }}
        />
        <Label htmlFor={checkboxId} className="cursor-pointer font-medium">
          {t("timesheet.massupdate.updateTask")}
        </Label>
      </div>
      <TaskKost2Picker
        taskId={taskId}
        kost2Id={kost2Id}
        onTaskChange={(ref) => setTaskId(ref?.id ?? null)}
        onKost2Change={setKost2Id}
        taskDisabled={!enabled}
      />
    </div>
  );
}
