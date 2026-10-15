import { create } from "zustand";

interface JobState {
  /** Ids of the backend jobs whose progress is shown as a toast (see components/shared/jobs/). */
  watchedJobIds: number[];
  watchJob: (id: number) => void;
  unwatchJob: (id: number) => void;
}

/**
 * Global on purpose: a re-index toast has to outlive the list page that started it, and the page is
 * unmounted the moment the user navigates away.
 */
export const useJobStore = create<JobState>((set) => ({
  watchedJobIds: [],
  watchJob: (id) =>
    set((s) =>
      s.watchedJobIds.includes(id)
        ? s
        : { watchedJobIds: [...s.watchedJobIds, id] }
    ),
  unwatchJob: (id) =>
    set((s) => ({ watchedJobIds: s.watchedJobIds.filter((v) => v !== id) })),
}));
