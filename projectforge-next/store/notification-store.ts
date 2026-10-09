import { create } from "zustand";

interface NotificationState {
  /** Ids of the notifications closed as toast or banner in this tab (still listed in the bell). */
  closedIds: number[];
  closeNotification: (id: number) => void;
}

/**
 * Global, as the toasts live in the layout and the banners in every page's shell: a banner closed on
 * one page must stay closed on the next one. Not persisted: a reload shows the open ones again.
 */
export const useNotificationStore = create<NotificationState>((set) => ({
  closedIds: [],
  closeNotification: (id) =>
    set((s) =>
      s.closedIds.includes(id) ? s : { closedIds: [...s.closedIds, id] }
    ),
}));
