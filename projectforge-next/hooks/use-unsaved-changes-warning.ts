"use client";

import { useEffect, useSyncExternalStore } from "react";
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
 * The in-app "leave anyway?" currently on screen, if any, and the resolver its buttons answer. Watched
 * by the one mounted dialog (see [useUnsavedChangesRequest]) so the question is the app's own styled
 * dialog rather than the browser's `window.confirm` box. There is only ever one, for the same reason
 * `pending` is a single slot.
 */
let request: { message: string; resolve: (leave: boolean) => void } | null =
  null;
const listeners = new Set<() => void>();

function emit(): void {
  for (const listener of listeners) listener();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/**
 * Warns before what is being edited is thrown away — the counterpart of Wicket's own "are you sure"
 * on an edit form.
 *
 * Two ways out of the form need it, and neither can be caught by keeping the values somewhere: the
 * link back to the legacy page (`LegacyPageLink`, deliberately a full reload) and a link out of the
 * form into another entity (an invoice position's order). The first is the browser's business, hence
 * `beforeunload`; the second is a client navigation, which [confirmLeaveUnsavedChanges] guards with
 * the app's own dialog.
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

/** Whether a form on screen holds unsaved changes — a synchronous check for a link's `onNavigate`. */
export function hasUnsavedChanges(): boolean {
  return pending !== null;
}

/**
 * Asks whether the edit form on screen may be left, and resolves true when there is nothing to lose.
 *
 * A promise, not the boolean `window.confirm` returned: the question is the app's own dialog (see
 * [useUnsavedChangesRequest]), which answers asynchronously. A caller therefore holds the navigation
 * and goes through only once this resolves true — for a link, `preventDefault` then a `router.push`:
 *
 * ```tsx
 * onNavigate={(e) => {
 *   if (!hasUnsavedChanges()) return;
 *   e.preventDefault();
 *   void confirmLeaveUnsavedChanges().then((leave) => { if (leave) router.push(href); });
 * }}
 * ```
 */
export function confirmLeaveUnsavedChanges(): Promise<boolean> {
  if (!pending) return Promise.resolve(true);
  // A second ask while one is still open answers the first as "stay" — one question at a time.
  request?.resolve(false);
  const { message } = pending;
  return new Promise<boolean>((resolve) => {
    request = { message, resolve };
    emit();
  });
}

/** Answers the open question (if any) and closes the dialog. Idempotent: a second call is a no-op. */
export function resolveUnsavedChanges(leave: boolean): void {
  const current = request;
  request = null;
  emit();
  current?.resolve(leave);
}

/** The open "leave anyway?" question, or null — for the one mounted dialog that renders it. */
export function useUnsavedChangesRequest(): { message: string } | null {
  return useSyncExternalStore(
    subscribe,
    () => request,
    () => null
  );
}
