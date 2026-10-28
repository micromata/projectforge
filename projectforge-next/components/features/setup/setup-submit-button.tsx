"use client";

import { useQuery } from "@tanstack/react-query";
import { Spinner } from "@/components/shared/spinner";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { useElapsedSeconds } from "@/hooks/use-elapsed-seconds";
import { fetchSetupProgress } from "@/lib/rs/setup";

interface SetupSubmitButtonProps {
  isSubmitting: boolean;
  /** Label of the idle button. */
  label: string;
  /** Label while the setup is running. */
  submittingLabel: string;
}

/**
 * Submit button of the setup form. The setup with test data takes a while, so while it runs the button shows a
 * spinner, the progress the server reports and the seconds passed, with a progress bar above (below, it could end
 * up outside the visible scroll area).
 */
export function SetupSubmitButton({
  isSubmitting,
  label,
  submittingLabel,
}: SetupSubmitButtonProps) {
  const elapsed = useElapsedSeconds(isSubmitting);
  const progress = useQuery({
    queryKey: ["setup", "progress"],
    queryFn: ({ signal }) => fetchSetupProgress(signal),
    enabled: isSubmitting,
    refetchInterval: 1_000,
    gcTime: 0,
  });
  const percent =
    isSubmitting && progress.data?.running
      ? (progress.data.percent ?? null)
      : null;
  const details = [
    percent != null ? `${percent} %` : null,
    elapsed > 0 ? `${elapsed} s` : null,
  ]
    .filter(Boolean)
    .join(" · ");
  const text = isSubmitting
    ? [submittingLabel, details].filter(Boolean).join(" ")
    : label;

  return (
    <>
      {percent != null && (
        <Progress value={percent} aria-label={submittingLabel} />
      )}
      <Button
        type="submit"
        className="w-full gap-2"
        disabled={isSubmitting}
        aria-busy={isSubmitting}
      >
        {isSubmitting && (
          <Spinner className="h-4 w-4 border-2 border-primary-foreground/30 border-t-primary-foreground" />
        )}
        {text}
      </Button>
    </>
  );
}
