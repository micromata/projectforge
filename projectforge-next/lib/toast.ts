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
 * The identity of an error message: its text.
 *
 * Sonner shows one toast per call, so a save refused three times says the same sentence three times and
 * pushes everything else out of the corner — the more so now that an error stays half a minute. Given the
 * text as its id, the second call *renews* the one that is there instead (sonner updates it in place and
 * restarts its timer, see `ToastState.create`), which is what "it happened again" looks like when the
 * message is word for word the one still standing.
 *
 * Only a string can be identified this way; a message built as JSX gets sonner's counter as before, and a
 * caller passing an `id` of its own keeps it (see the spread below).
 */
function errorToastId(message: unknown): string | undefined {
  return typeof message === "string" ? `error:${message}` : undefined;
}

/**
 * Sonner's `toast`, with the defaults every failure of this app is reported with: [ERROR_TOAST_DURATION],
 * a close button so a message that stays that long can be got rid of, and one toast per text
 * ([errorToastId]).
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
    error: ((message, options) =>
      sonnerToast.error(message, {
        duration: ERROR_TOAST_DURATION,
        closeButton: true,
        id: errorToastId(message),
        ...options,
      })) as typeof sonnerToast.error,
  }
);
