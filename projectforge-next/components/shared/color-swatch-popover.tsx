"use client";

import { useRef, useState } from "react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { ColorPicker } from "@/components/shared/color-picker";

interface ColorSwatchPopoverProps {
  /** The current colour as a hex string; empty renders a neutral, transparent swatch. */
  value: string;
  /** Reports the chosen colour once, when the popover closes (see the commit-on-close note below). */
  onChange: (color: string) => void;
  /** Accessible name of the swatch button — the field it colours. */
  "aria-label": string;
}

/**
 * A compact colour control: a small round swatch that opens a popover holding the shared
 * {@link ColorPicker}. The colour is committed once, when the popover closes, so dragging across the
 * palette is a single `onChange` and not one per swatch — the same commit-on-close the calendar pills
 * use (see calendar-pill). Reusable anywhere a full inline picker would be too heavy.
 */
export function ColorSwatchPopover({
  value,
  onChange,
  "aria-label": ariaLabel,
}: ColorSwatchPopoverProps) {
  // The colour the popover is showing; reported to the caller only when it closes.
  const [pendingColor, setPendingColor] = useState(value);
  const committed = useRef(value);

  const onOpenChange = (open: boolean) => {
    if (open) {
      setPendingColor(value);
      committed.current = value;
      return;
    }
    if (pendingColor !== committed.current) {
      committed.current = pendingColor;
      onChange(pendingColor);
    }
  };

  return (
    <Popover onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label={ariaLabel}
          className="size-5 shrink-0 cursor-pointer rounded-md border border-border"
          style={{ background: value || "transparent" }}
        />
      </PopoverTrigger>
      <PopoverContent align="start" className="w-auto">
        <ColorPicker
          value={pendingColor}
          onChange={setPendingColor}
          aria-label={ariaLabel}
        />
      </PopoverContent>
    </Popover>
  );
}
