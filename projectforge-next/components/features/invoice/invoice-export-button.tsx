"use client";

import type { ComponentProps } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";

export interface InvoiceExportButtonProps extends Omit<
  ComponentProps<typeof Button>,
  "children" | "disabled"
> {
  /**
   * What the export does, or — for a disabled one — why it cannot be had (see [InvoiceExportMenu]).
   *
   * Optional, for a button whose label says everything there is to say: the e-invoice section's two
   * ("Speichern und XRechnung", see [EInvoiceActions]).
   */
  tooltip?: string;
  label: string;
  /** Replaces the icon with a spinner: an export runs for seconds, so it has to say that it is running. */
  isPending: boolean;
  disabled?: boolean;
}

/**
 * A button that fetches a file — the shape every export of this feature has: the two of the list toolbar
 * and the Word export of the edit page.
 *
 * Shared rather than repeated because the *state* is the interesting part: a download is a mutation with a
 * pending phase and no visible result on the page, so the spinner in place of the download icon is the only
 * feedback there is until the browser's own download appears.
 *
 * Everything else a button takes is passed through, which is what lets this be the trigger of a menu as
 * well ([InvoiceExportMenu]): `DropdownMenuTrigger asChild` clones its child with the handlers and the
 * `aria-*` state that open it, and a component that named its props one by one would swallow them.
 */
export function InvoiceExportButton({
  tooltip,
  label,
  isPending,
  disabled,
  ...props
}: InvoiceExportButtonProps) {
  const button = (
    <Button
      type="button"
      variant="ghost"
      size="sm"
      className="gap-1.5"
      disabled={isPending || disabled}
      {...props}
    >
      {isPending ? (
        <Spinner className="h-3.5 w-3.5 border-2" />
      ) : (
        <HugeiconsIcon icon={Download04Icon} size={14} aria-hidden />
      )}
      {label}
    </Button>
  );
  return (
    <HintTooltip text={tooltip}>
      {/* Wrapped while disabled: the primitive sets `pointer-events-none` on a disabled button, so it
          receives no hover — and a disabled export is exactly the case where the tooltip carries the
          only explanation there is (see [InvoiceExportMenu]). */}
      {disabled ? <span tabIndex={0}>{button}</span> : button}
    </HintTooltip>
  );
}
