"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/shared/spinner";

export interface WizardFinishButtonProps {
  disabled?: boolean;
  isPending: boolean;
  onFinish: () => void;
  /** The text of the button, the structure wizard's by default. */
  label?: string;
}

/** The primary button of a wizard, with a spinner in place of its icon while it runs. */
export function WizardFinishButton({
  disabled,
  isPending,
  onFinish,
  label,
}: WizardFinishButtonProps) {
  const t = useTranslations();
  return (
    <Button
      type="button"
      disabled={disabled || isPending}
      onClick={onFinish}
      className="gap-1.5"
      aria-busy={isPending}
    >
      {/* In place of the icon, not next to it, so the label doesn't move (see EntityEditActions).
          The grant itself is a handful of rows and thus quick, but it writes them one by one over
          the whole path up to the root. */}
      {isPending ? (
        <Spinner className="h-3.5 w-3.5 border-2" />
      ) : (
        <HugeiconsIcon icon={Tick02Icon} size={14} />
      )}
      {label ?? t("task.wizard.finish")}
    </Button>
  );
}
