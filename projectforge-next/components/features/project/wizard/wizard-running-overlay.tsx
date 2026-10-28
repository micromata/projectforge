"use client";

import { Progress } from "@/components/ui/progress";
import { useElapsedSeconds } from "@/hooks/use-elapsed-seconds";

/**
 * Laid over the wizard while Finish runs: one request, which writes customer, project, elements, groups
 * and rights at once and reports no progress of its own. So the bar is the moving one of the uploads
 * (not a percentage), with the seconds passed, which says the request is still alive; and the form
 * below can't be changed meanwhile.
 */
export function WizardRunningOverlay({ label }: { label: string }) {
  const elapsed = useElapsedSeconds(true);
  return (
    <div className="absolute inset-0 z-20 flex items-center justify-center bg-background/70">
      <div
        role="status"
        aria-live="polite"
        className="flex w-80 max-w-[90%] flex-col gap-3 rounded-lg border bg-card p-4 shadow-lg"
      >
        <p className="text-sm font-medium">
          {label}
          {elapsed > 0 && (
            <span className="text-muted-foreground"> · {elapsed} s</span>
          )}
        </p>
        {/* upload-progress styles the fill from the outside (globals.css): moving stripes. */}
        <Progress
          value={100}
          aria-label={label}
          className="upload-progress upload-progress-active h-2"
        />
      </div>
    </div>
  );
}
