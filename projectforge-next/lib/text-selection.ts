import { useRef, type MouseEvent, type PointerEvent } from "react";

/**
 * Whether the user has selected text inside `element` — what the click ending a drag across a
 * picker's value leaves behind. Such a click is the user copying, not asking to open the picker.
 */
export function hasTextSelectionIn(element: Element): boolean {
  const selection = window.getSelection();
  return (
    selection != null &&
    !selection.isCollapsed &&
    selection.anchorNode != null &&
    element.contains(selection.anchorNode)
  );
}

/**
 * Tells the click that ends a drag across a picker's value — the user selecting it to copy — from a
 * click that is to open the picker. A selection alone doesn't say: it is still there on a click onto
 * a value selected before, and that click is to open the picker as any other.
 *
 * `onPointerDown` goes on the element the drag starts in, `endsSelectingDrag` is asked in the click.
 */
export function useSelectingDrag() {
  const down = useRef<{ x: number; y: number } | null>(null);
  return {
    onPointerDown(event: PointerEvent) {
      down.current = { x: event.clientX, y: event.clientY };
    },
    endsSelectingDrag(event: MouseEvent<Element>): boolean {
      const start = down.current;
      down.current = null;
      if (!start) return false;
      const moved =
        Math.abs(event.clientX - start.x) > 2 ||
        Math.abs(event.clientY - start.y) > 2;
      return moved && hasTextSelectionIn(event.currentTarget);
    },
  };
}

/**
 * The classes for a picker trigger whose value is to be copyable: `Button` makes its text
 * `select-none`, and a read-only trigger is dimmed by `aria-disabled` (see useCopyableTrigger).
 */
export const COPYABLE_TRIGGER_CLASS =
  "select-text aria-disabled:cursor-default aria-disabled:opacity-50";

/**
 * Props for a popover trigger `<button>` whose value can be selected and copied with the mouse:
 * the click ending a drag across the value doesn't open the popover (Radix skips its toggle on a
 * prevented click). Not `disabled` for a read-only picker but `aria-disabled` — a disabled button
 * gets no mouse events, so its text could not be selected at all; the popover is kept shut by
 * cancelling the click here and by whoever owns its open state (see useTypeToOpen).
 */
export function useCopyableTrigger(disabled?: boolean) {
  const { onPointerDown, endsSelectingDrag } = useSelectingDrag();
  return {
    "aria-disabled": disabled || undefined,
    onPointerDown,
    onClick: (event: MouseEvent<HTMLElement>) => {
      if (endsSelectingDrag(event) || disabled) event.preventDefault();
    },
  };
}
