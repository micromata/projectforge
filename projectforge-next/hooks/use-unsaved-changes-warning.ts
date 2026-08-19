"use client";

import { useEffect } from "react";
import { useTranslations } from "next-intl";

/**
 * Whether a form on screen holds unsaved changes, and how to ask about leaving it.
 *
 * A module variable rather than a context, because the asking happens in an event handler that is not
 * a component: `onNavigate` of a link, which has no hooks to read a context with. There is at most one
 * edit form on screen, so one slot suffices.
 */
let pending: { message: string } | null = null;

/**
 * Warns before what is being edited is thrown away — the counterpart of Wicket's own "are you sure"
 * on an edit form.
 *
 * Two ways out of the form need it, and neither can be caught by keeping the values somewhere: the
 * link back to the legacy page (`LegacyPageLink`, deliberately a full reload) and a link out of the
 * form into another entity (an invoice position's order). The first is the browser's business, hence
 * `beforeunload`; the second is a client navigation, which [confirmLeaveUnsavedChanges] guards.
 *
 * @param isDirty whether the form holds changes. False disarms both, so a saved or untouched form
 *   leaves without a word.
 */
export function useUnsavedChangesWarning(isDirty: boolean): void {
  const t = useTranslations();
  const message = t("question.leaveUnsavedChanges");

  useEffect(() => {
    if (!isDirty) return;
    pending = { message };
    // `preventDefault` is what asks for the browser's own dialog; its text is the browser's and cannot
    // be set (every browser ignores a returned string by now), which is why the message above is only
    // used for the in-app case.
    const onBeforeUnload = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => {
      pending = null;
      window.removeEventListener("beforeunload", onBeforeUnload);
    };
  }, [isDirty, message]);
}

/**
 * Asks whether the edit form on screen may be left, and answers true when there is nothing to lose.
 *
 * For the `onNavigate` of a link that leads out of an edit form — see [useUnsavedChangesWarning]:
 *
 * ```tsx
 * <Link onNavigate={(e) => { if (!confirmLeaveUnsavedChanges()) e.preventDefault(); }} … />
 * ```
 */
export function confirmLeaveUnsavedChanges(): boolean {
  return !pending || window.confirm(pending.message);
}
