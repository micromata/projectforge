"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Copy01Icon, TickDouble01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { InputGroupButton } from "@/components/ui/input-group";
import { useCopyToClipboard } from "@/hooks/use-copy-to-clipboard";
import { cn } from "@/lib/utils";

interface Props {
  value: string | null | undefined;
  /** What is copied, for the accessible name ("Copy: password"); without it just "Copy". */
  label?: string;
  /** `ghost` and small for a value inside a text (see AttachmentMetadata), `outline` beside a box. */
  variant?: "outline" | "ghost";
  /** Small and borderless, inside the box it copies from (SecretInput, ReadonlyValue); `variant` is ignored. */
  inline?: boolean;
  className?: string;
  iconSize?: number;
}

/**
 * Puts a value on the clipboard. The tick that says it worked goes away after a moment (see
 * useCopyToClipboard), so the same value can be copied again. Disabled without a value.
 */
export function CopyButton({
  value,
  label,
  variant = "outline",
  inline,
  className,
  iconSize = inline ? 14 : 16,
}: Props) {
  const t = useTranslations();
  const { copied, copy } = useCopyToClipboard();
  const action = copied ? t("copied") : t("copy");
  const props = {
    type: "button" as const,
    className: cn("shrink-0", className),
    "aria-label": label ? `${action}: ${label}` : action,
    disabled: !value,
    onClick: () => void copy(value),
    children: (
      <HugeiconsIcon
        icon={copied ? TickDouble01Icon : Copy01Icon}
        size={iconSize}
      />
    ),
  };
  return inline ? (
    <InputGroupButton size="icon-xs" {...props} />
  ) : (
    <Button variant={variant} size="icon" {...props} />
  );
}
