import type { ResponseAction } from "@/lib/rs/types";

/**
 * What happens after each way an edit form finishes — the one thing a page and a modal do
 * differently.
 *
 * The shared body ([EntityEditBody]) builds the form, the sections, the delete and the clone the same
 * for both; only *leaving* differs. On its own page each of these navigates (`router.push(back.route)`),
 * in a dialog each closes the dialog and hands the result to its caller. Injecting the five here is what
 * lets one body serve both, instead of the page and the modal each carrying a copy of the form.
 *
 * `afterSave` gets the id the backend assigned (null if it named none) and the values that were saved,
 * since the answer of a write carries no entity (see lib/rs/entity.ts) and a caller usually wants a
 * name as well as an id. It also gets the write's `ResponseAction`: the calendar reads its
 * `?gotoDate=…&hash=…` redirect url to jump to the saved entry's period (see CalendarEditRouteClient);
 * a page host ignores it. `afterClone` gets the add route the clone was prepared under
 * (`/{entity}/new?clone=1`), which a page navigates to and a dialog opens as its own page.
 */
export interface EditOutcome {
  afterSave: (
    id: number | null,
    values: unknown,
    action?: ResponseAction
  ) => void;
  afterCancel: () => void;
  afterDelete: () => void;
  afterUndelete: () => void;
  afterClone: (route: string) => void;
}
