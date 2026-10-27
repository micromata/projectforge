import type {
  Kost2ReallocationRef,
  Kost2ReallocationRule,
} from "@/lib/rs/kost2-reallocation";

/** The definition of a run as the first step edits it (the same fields `Kost2ReallocationRest.RunData` saves). */
export interface RunDraft {
  description: string;
  projekt: Kost2ReallocationRef | null;
  taskId: number | null;
  fromYear: number | null;
  fromMonth: number | null;
  toYear: number | null;
  toMonth: number | null;
  comment: string;
  rules: Kost2ReallocationRule[];
}

export const emptyRunDraft = (): RunDraft => {
  const now = new Date();
  // A reallocation concerns booked months: the previous one by default.
  const previous = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  return {
    description: "",
    projekt: null,
    taskId: null,
    fromYear: previous.getFullYear(),
    fromMonth: previous.getMonth() + 1,
    toYear: previous.getFullYear(),
    toMonth: previous.getMonth() + 1,
    comment: "",
    rules: [],
  };
};
