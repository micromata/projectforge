"use client";

import { toast as sonnerToast } from "sonner";

/**
 * How long a message reporting a failure stays: long enough to read a sentence that names a field and a
 * rule, look back at the form, and read it again.
 *
 * Sonner's four seconds are the right length for "saved" — nothing is lost when that one is missed. An
 * error is the opposite: it is the only account of why the save did *not* happen, and a user who looks
 * down at the field the message is about has already lost it.
 */
export const ERROR_TOAST_DURATION = 30_000;

/**
 * Closes any error toast showing this exact text right now, so the caller can then open a fresh one in
 * its place.
 *
 * Sonner shows one toast per call, so a save refused three times says the same sentence three times and
 * pushes everything else out of the corner — the more so now that an error stays half a minute. Rather
 * than suppress the repeat (which loses the "it happened again" and, done by a fixed id, kept swallowing
 * a message the user had already clicked away), we dismiss the standing copy and let a new toast take
 * its place — so the latest is always on top and there is never a duplicate.
 *
 * "Right now" is why this asks sonner rather than remembering ids: `getToasts()` lists only the live
 * toasts — a dismissed or timed-out one has already left it (unlike `getHistory()`, which keeps every
 * toast ever shown) — so a text no longer on screen matches nothing here and its next error simply opens
 * a toast, none to clear first.
 */
function dismissStandingErrorToasts(message: string): void {
  sonnerToast
    .getToasts()
    .filter((shown) => "title" in shown && shown.title === message)
    .forEach((shown) => sonnerToast.dismiss(shown.id));
}

/**
 * Sonner's `toast`, with the defaults every failure of this app is reported with: [ERROR_TOAST_DURATION],
 * a close button so a message that stays that long can be got rid of, and only ever one toast per text on
 * screen ([dismissStandingErrorToasts]).
 *
 * Wrapped rather than passed at the call sites, and rather than set on the `Toaster`: there are two dozen
 * places reporting a failure and there will be more, `Toaster` has one duration for all types (which
 * would make "saved" stand around for half a minute too), and a default that has to be repeated is a
 * default the next call site will not have. Import `toast` from here, never from "sonner" — the eslint
 * rule in eslint.config.mjs says so as well.
 *
 * Everything else is sonner's, unchanged: an option given at the call site wins (see
 * lib/dynamic/response-toast.ts, whose messages stay until they are closed).
 */
export const toast: typeof sonnerToast = Object.assign(
  ((...args: Parameters<typeof sonnerToast>) =>
    sonnerToast(...args)) as typeof sonnerToast,
  sonnerToast,
  {
    error: ((message, options) => {
      const defaults = { duration: ERROR_TOAST_DURATION, closeButton: true };
      // A JSX message can't be keyed by text: show it as sonner would, with our defaults only.
      if (typeof message !== "string") {
        return sonnerToast.error(message, { ...defaults, ...options });
      }
      // Clear any copy of this text that is still up, then show a fresh toast — the latest error stays
      // on top and no duplicate piles up behind it (see dismissStandingErrorToasts). A caller giving its
      // own id opts out: it manages that toast itself.
      if (options?.id == null) dismissStandingErrorToasts(message);
      return sonnerToast.error(message, { ...defaults, ...options });
    }) as typeof sonnerToast.error,
  }
);
