"use client";

import { TaskWizardLink } from "@/components/shared/tasks/task-wizard-link";

/**
 * The access list's toolbar action — the task ("structure") wizard, the same "Assistent" the Wicket
 * `AccessListPage` offered its admins. Reuses the shared [TaskWizardLink], which renders nothing for a
 * non-admin (its endpoints check the same), so no gating is needed here.
 *
 * Through `PageDef.listActions`, the declared slot left of the gear menu; the handed-in filter is not
 * read — the wizard sets up a whole project's rights and does not act on the current list rows.
 */
export function AccessListActions() {
  return <TaskWizardLink />;
}
