"use client";

import { useCallback, useRef, useState } from "react";
import {
  uploadAttachment,
  type AttachmentWriteResult,
} from "@/lib/rs/attachments";
import { UploadError } from "@/lib/rs/upload";
import { useAttachmentService } from "@/components/shared/attachments/attachment-service";

/** One file on its way to the server, as the UI needs to show it. */
export interface UploadJob {
  /** Stable within the session — the file name is not unique enough while a rename is possible. */
  id: number;
  name: string;
  size: number;
  /** 0-100, or null while the browser cannot tell the total. */
  percent: number | null;
  /** Smoothed upload speed in bytes per second, or null until two progress events allow a measurement. */
  bytesPerSecond: number | null;
  /** waiting → sending → the server's verdict. */
  state: "queued" | "sending" | "failed";
  /** Why it failed; already translated when it comes from the backend. */
  error?: string;
}

export interface UseAttachmentUploadsOptions {
  /** Called with each finished upload's answer, so the caller can update the list and report a refusal. */
  onResult: (result: AttachmentWriteResult, fileName: string) => void;
  /** Message for a transfer that never reached the backend (network, proxy, abort). */
  transferErrorMessage: string;
  /** Message for a file larger than `AttachmentService.maxFileSize`, which is then not sent at all. */
  tooLargeMessage?: string;
}

let nextJobId = 0;

/** Progress events closer together than this are folded into the next one — their rate would be noise. */
const MIN_SAMPLE_MILLIS = 250;
/** Weight of the newest sample in the smoothed speed. */
const SMOOTHING = 0.3;

/**
 * Uploads files in parallel and tracks each one's progress separately, as the legacy
 * `MultipleFileUploadArea` did: several files mean several bars, all filling at once.
 *
 * The endpoint still takes a single file per call, so this fans out one call per file. Files sharing
 * a *name* are the one exception and go one after the other: the backend's duplicate check runs
 * against what is already stored, so two simultaneous calls could let the same name through twice.
 * That only ever affects a re-upload of the same name, never a normal multi-selection.
 *
 * A job vanishes from the list once it succeeded — the file then appears in the attachment list
 * itself, and keeping both would say the same thing twice. Failed jobs stay until dismissed, since
 * nothing else on screen would mention them.
 */
export function useAttachmentUploads(
  entity: string,
  id: number | null,
  {
    onResult,
    transferErrorMessage,
    tooLargeMessage,
  }: UseAttachmentUploadsOptions
) {
  const { base, onWriteError, maxFileSize } = useAttachmentService();
  const [jobs, setJobs] = useState<UploadJob[]>([]);
  const controllers = useRef(new Map<number, AbortController>());
  /** Jobs cancelled before their turn came — [send] checks this instead of starting the transfer. */
  const cancelled = useRef(new Set<number>());
  /**
   * Per file name, the upload currently occupying that name — the next one with the same name
   * chains onto it instead of racing it. A ref, so a second `enqueue` in the same tick sees it.
   */
  const chains = useRef(new Map<string, Promise<void>>());

  const patch = useCallback((jobId: number, changes: Partial<UploadJob>) => {
    setJobs((current) =>
      current.map((job) => (job.id === jobId ? { ...job, ...changes } : job))
    );
  }, []);

  const send = useCallback(
    async (job: UploadJob, file: File) => {
      // Cancelled while it waited for another upload of the same name: never send it at all.
      if (cancelled.current.delete(job.id)) return;
      const controller = new AbortController();
      controllers.current.set(job.id, controller);
      patch(job.id, { state: "sending", percent: 0, bytesPerSecond: null });
      let sample = { time: performance.now(), loaded: 0 };
      let speed: number | null = null;
      try {
        const result = await uploadAttachment(
          entity,
          id!,
          file,
          undefined,
          {
            onProgress: ({ percent, loaded }) => {
              const now = performance.now();
              if (now - sample.time >= MIN_SAMPLE_MILLIS) {
                const current =
                  ((loaded - sample.loaded) / (now - sample.time)) * 1000;
                speed =
                  speed == null
                    ? current
                    : SMOOTHING * current + (1 - SMOOTHING) * speed;
                sample = { time: now, loaded };
              }
              patch(job.id, { percent, bytesPerSecond: speed });
            },
            signal: controller.signal,
          },
          base
        );
        onResult(result, file.name);
        if (result.kind === "rejected") {
          patch(job.id, { state: "failed", error: result.message });
        } else {
          // Succeeded: the file is in the list now, so the bar has nothing left to say.
          setJobs((current) => current.filter((j) => j.id !== job.id));
        }
      } catch (error) {
        // An abort is the user's own doing, so it leaves nothing behind to dismiss.
        if (error instanceof UploadError && error.aborted) {
          setJobs((current) => current.filter((j) => j.id !== job.id));
        } else {
          patch(job.id, { state: "failed", error: transferErrorMessage });
          onWriteError?.();
        }
      } finally {
        controllers.current.delete(job.id);
      }
    },
    [base, entity, id, onResult, onWriteError, patch, transferErrorMessage]
  );

  const enqueue = useCallback(
    (files: File[]) => {
      if (id == null || id <= 0 || files.length === 0) return;
      const added = files.map((file) => {
        // Refused before the transfer: the backend would only say so once the whole file had arrived.
        const tooLarge = maxFileSize != null && file.size > maxFileSize.bytes;
        const job: UploadJob = {
          id: (nextJobId += 1),
          name: file.name,
          size: file.size,
          percent: null,
          bytesPerSecond: null,
          state: tooLarge ? "failed" : "queued",
          error: tooLarge ? tooLargeMessage : undefined,
        };
        return { job, file };
      });
      setJobs((current) => [...current, ...added.map((entry) => entry.job)]);

      added.forEach(({ job, file }) => {
        if (job.state === "failed") return;
        const previous = chains.current.get(file.name);
        // `catch` on the predecessor: a failed upload must not keep the next one of that name from
        // being tried at all.
        const next = previous
          ? previous.then(
              () => send(job, file),
              () => send(job, file)
            )
          : send(job, file);
        chains.current.set(file.name, next);
        void next.finally(() => {
          // Last of its name: drop the entry, so the map doesn't grow for the page's lifetime.
          if (chains.current.get(file.name) === next) {
            chains.current.delete(file.name);
          }
        });
      });
    },
    [id, maxFileSize, send, tooLargeMessage]
  );

  /** Cancels a running upload or drops a queued/failed one. */
  const cancel = useCallback((jobId: number) => {
    const controller = controllers.current.get(jobId);
    if (controller) {
      // The abort handler removes the job; doing it here too would race with it.
      controller.abort();
      return;
    }
    // Queued behind another upload of the same name, or already failed: no transfer to abort, so it
    // is enough to drop the row and remember not to start it (see send).
    cancelled.current.add(jobId);
    setJobs((current) => current.filter((job) => job.id !== jobId));
  }, []);

  return { jobs, enqueue, cancel, isUploading: jobs.length > 0 };
}
