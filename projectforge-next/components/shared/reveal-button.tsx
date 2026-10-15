"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ViewIcon, ViewOffSlashIcon } from "@hugeicons/core-free-icons";
import { InputGroupButton } from "@/components/ui/input-group";

interface Props {
  revealed: boolean;
  onRevealedChange: (revealed: boolean) => void;
  /** Names the secret in the accessible name ("Show: password"). */
  label: string;
  disabled?: boolean;
}

/** The eye inside a secret's box (SecretInput, ReadonlyValue): shows the secret, and hides it again. */
export function RevealButton({
  revealed,
  onRevealedChange,
  label,
  disabled,
}: Props) {
  const t = useTranslations();
  return (
    <InputGroupButton
      type="button"
      size="icon-xs"
      className="shrink-0"
      aria-label={`${revealed ? t("secret.hide") : t("secret.show")}: ${label}`}
      aria-pressed={revealed}
      disabled={disabled}
      onClick={() => onRevealedChange(!revealed)}
    >
      <HugeiconsIcon icon={revealed ? ViewOffSlashIcon : ViewIcon} size={14} />
    </InputGroupButton>
  );
}
