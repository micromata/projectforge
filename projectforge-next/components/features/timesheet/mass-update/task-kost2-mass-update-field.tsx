"use client";

import { useEffect, useState } from "react";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
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
 * `taskAndKost2.change` is set as soon as a task is picked — entering a value *is* the request to change
 * it here, as for every other mass-update field; there is no separate opt-in checkbox. Clearing the task
 * removes all three keys, so no action is left behind. The cost unit is validated against the task server
 * side (`handleClientMassUpdateCall`), and a task-only change maps the cost unit to the same type in the
 * new project — so posting just the task is a complete instruction.
 */
export function TaskKost2MassUpdateField({
  setParam,
}: {
  setParam: (name: string, param: MassUpdateParameter | undefined) => void;
}) {
  const [task, setTask] = useState<EntityRef | null>(null);
  const [kost2Id, setKost2Id] = useState<number | null>(null);

  useEffect(() => {
    if (task == null) {
      setParam("task", undefined);
      setParam("kost2", undefined);
      setParam("taskAndKost2", undefined);
      return;
    }
    setParam("task", { id: task.id });
    setParam("kost2", kost2Id != null ? { id: kost2Id } : undefined);
    setParam("taskAndKost2", { change: true });
    // `setParam` is stable (a useCallback in MassUpdateForm); the ids are all that drive the parameters.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [task, kost2Id]);

  return (
    <TaskKost2Picker
      taskId={task?.id ?? null}
      kost2Id={kost2Id}
      onTaskChange={setTask}
      onKost2Change={setKost2Id}
    />
  );
}
