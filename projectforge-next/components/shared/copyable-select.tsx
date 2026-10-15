"use client";

import * as React from "react";
import {
  Select as UiSelect,
  SelectTrigger as UiSelectTrigger,
} from "@/components/ui/select";
import { useSelectingDrag } from "@/lib/text-selection";
import { cn } from "@/lib/utils";

/**
 * The shadcn `Select` and `SelectTrigger` (components/ui/select.tsx), with the value shown in the
 * trigger selectable and copyable by mouse. Same names and props, so a caller only changes the
 * import; the other parts (`SelectContent`, `SelectItem`, `SelectValue`) are taken from ui as they
 * are.
 *
 * Radix opens the select on a mouse pointerdown, and its modal content leaves no chance for a drag
 * across the text. So a pointerdown on the value is kept from Radix, and the click following it
 * opens the select — unless it ended a drag that selected text (see useSelectingDrag). Padding,
 * chevron, keyboard and touch work as Radix has them.
 */

/** Lets [SelectTrigger] open the select itself. */
const SelectOpenContext = React.createContext<((open: boolean) => void) | null>(
  null
);

/** Holds the open state itself, unless the caller controls it, for [SelectTrigger] to set. */
export function Select({
  open: openProp,
  defaultOpen,
  onOpenChange,
  ...props
}: React.ComponentProps<typeof UiSelect>) {
  const [openState, setOpenState] = React.useState(defaultOpen ?? false);
  const setOpen = React.useCallback(
    (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    },
    [openProp, onOpenChange]
  );
  return (
    <SelectOpenContext.Provider value={setOpen}>
      <UiSelect
        open={openProp ?? openState}
        onOpenChange={setOpen}
        {...props}
      />
    </SelectOpenContext.Provider>
  );
}

export function SelectTrigger({
  className,
  children,
  onClick,
  ...props
}: React.ComponentProps<typeof UiSelectTrigger>) {
  const setOpen = React.useContext(SelectOpenContext);
  const selectingDrag = useSelectingDrag();
  /** Whether the click is one whose pointerdown on the value was kept from Radix. */
  const fromText = React.useRef(false);
  return (
    <UiSelectTrigger
      className={cn("select-text", className)}
      onClick={(event) => {
        onClick?.(event);
        const ours = fromText.current;
        fromText.current = false;
        if (event.defaultPrevented || !ours) return;
        // Ours alone: Radix never saw the pointerdown, so it would take the click for a touch and
        // open the select even after a drag.
        event.preventDefault();
        if (selectingDrag.endsSelectingDrag(event) || props.disabled) return;
        event.currentTarget.focus();
        setOpen?.(true);
      }}
      {...props}
    >
      <span
        data-slot="select-trigger-text"
        // The value's classes of the ui trigger, which only reach its direct children. And pointer
        // events back on for it — important, to beat the `pointer-events: none` Radix sets inline on
        // the value (taking no className): a text that ignores the pointer cannot be selected by it.
        className="min-w-0 select-text **:data-[slot=select-value]:pointer-events-auto! **:data-[slot=select-value]:line-clamp-1 **:data-[slot=select-value]:flex **:data-[slot=select-value]:items-center **:data-[slot=select-value]:gap-1.5"
        onPointerDown={(event) => {
          fromText.current = false;
          if (
            event.button !== 0 ||
            event.ctrlKey ||
            event.pointerType !== "mouse"
          )
            return;
          fromText.current = true;
          selectingDrag.onPointerDown(event);
          event.stopPropagation();
        }}
      >
        {children}
      </span>
    </UiSelectTrigger>
  );
}
