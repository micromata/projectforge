"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Copy01Icon, TickDouble01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface Props {
  value: string | null | undefined;
  /** Accessible name of the box, as there is no visible label tied to it. */
  label: string;
  /**
   * Shown as dots, for a secret that is passed on but shouldn't be read over one's shoulder — the
   * password of a data transfer area. Copying still gives the value itself.
   */
  masked?: boolean;
  /** Further controls after the copy button — a data transfer link's "renew". */
  children?: ReactNode;
  className?: string;
}

/**
 * A value to pass on rather than to edit — a subscription url, the link of a data transfer area: a
 * read-only box, so it can still be selected by hand, and a button putting it on the clipboard. The
 * button's tick says it worked.
 */
export function CopyableValue({
  value,
  label,
  masked,
  children,
  className,
}: Props) {
  const t = useTranslations();
  const [copied, setCopied] = useState(false);

  async function copy() {
    if (!value) return;
    await navigator.clipboard.writeText(value);
    setCopied(true);
  }

  return (
    <div className={cn("flex items-center gap-2", className)}>
      <Input
        readOnly
        type={masked ? "password" : "text"}
        value={value ?? ""}
        aria-label={label}
      />
      <Button
        type="button"
        variant="outline"
        size="icon"
        className="shrink-0"
        aria-label={t("copy")}
        disabled={!value}
        onClick={() => void copy()}
      >
        <HugeiconsIcon
          icon={copied ? TickDouble01Icon : Copy01Icon}
          size={16}
        />
      </Button>
      {children}
    </div>
  );
}
