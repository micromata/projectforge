"use client";

import { useCallback, useState, type ComponentProps } from "react";

import { Dialog } from "@/components/ui/dialog";
import {
  confirmLeaveUnsavedChanges,
  useUnsavedChangesWarning,
} from "@/hooks/use-unsaved-changes-warning";

/**
 * A `Dialog` that asks before it is dismissed once the user has typed into it.
 *
 * A click beside the dialog, Esc or the close button all arrive as `onOpenChange(false)`; without a
 * guard they throw the input away. The dialog counts as dirty after the first input the user made
 * (`isTrusted`, so a preset the code wrote doesn't count), and then the app's own "leave anyway?"
 * is asked first. The dialog's own buttons set the parent's state directly and are not asked.
 *
 * Drop-in for `Dialog`; the events of the portalled content bubble to the wrapper element through
 * React's tree. Not for dialogs that already ask themselves (`EntityEditModal`).
 */
export function GuardedDialog({
  onOpenChange,
  open,
  children,
  ...props
}: ComponentProps<typeof Dialog>) {
  const [dirty, setDirty] = useState(false);
  useUnsavedChangesWarning(dirty && open !== false);

  // A dialog kept mounted and closed by the parent's own button starts clean the next time.
  const [wasOpen, setWasOpen] = useState(open);
  if (open !== wasOpen) {
    setWasOpen(open);
    if (open === false) setDirty(false);
  }

  const markDirty = useCallback((event: { isTrusted: boolean }) => {
    if (event.isTrusted) setDirty(true);
  }, []);

  const handleOpenChange = useCallback(
    (next: boolean) => {
      if (next || !dirty) {
        if (!next) setDirty(false);
        onOpenChange?.(next);
        return;
      }
      // Stays open while the question is answered; closes only on "leave".
      void confirmLeaveUnsavedChanges().then((leave) => {
        if (!leave) return;
        setDirty(false);
        onOpenChange?.(false);
      });
    },
    [dirty, onOpenChange]
  );

  return (
    <Dialog open={open} onOpenChange={handleOpenChange} {...props}>
      <div className="contents" onInputCapture={markDirty}>
        {children}
      </div>
    </Dialog>
  );
}
