"use client";

import { toast } from "@/lib/toast";
import { showResponseMessage } from "@/lib/dynamic/response-toast";
import { reindexFull, reindexNewest } from "@/lib/rs/list-actions";
import { useJobStore } from "@/store/job-store";

/**
 * Starts a re-index run of the search index and puts its progress on screen.
 *
 * The endpoints answer right away with the id of a background job (see startReindexOrRunIt in
 * AbstractPagesRest) — a full run takes minutes, so waiting for it would block the page. Handing the
 * id to the job store is all there is to do; the toast itself belongs to `JobToasts`, which is
 * mounted for the whole authenticated area and therefore survives leaving this page.
 */
export function useReindex(entity: string) {
  const watchJob = useJobStore((s) => s.watchJob);

  async function start(full: boolean): Promise<void> {
    try {
      const response = await (full
        ? reindexFull(entity)
        : reindexNewest(entity));
      const jobId = response.variables?.jobId;
      if (typeof jobId === "number") {
        watchJob(jobId);
      } else if (response.message) {
        // No job id means the server ran it synchronously and answered with the finished run's toast
        // (that is the path of the classic clients).
        showResponseMessage(response.message);
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : String(err));
    }
  }

  return { start };
}
