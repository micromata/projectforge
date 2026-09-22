"use client";

import { toast } from "@/lib/toast";
import type { ResponseActionMessage } from "@/lib/rs/types";

/**
 * Shows the message a `ResponseAction` came with (org.projectforge.ui.ResponseAction.Message).
 *
 * The text is already translated by the backend - `Message` resolves its i18nKey in the init block -
 * so there is nothing left to look up here. The color decides the toast variant, mirroring the
 * `UIColor` the server picked.
 */
export function showResponseMessage(message: ResponseActionMessage): void {
  const text = message.message ?? message.technicalMessage ?? message.i18nKey;
  if (!text) return;
  if (message.color === "danger") toast.error(text, STICKY);
  else if (message.color === "warning") toast.warning(text, STICKY);
  else if (message.color === "success") toast.success(text);
  else toast.info(text);
}

/**
 * Stays until the user closes it, for the two colors that report something going wrong.
 *
 * Such a message accompanies an operation that otherwise succeeded (an order saved whose notification
 * mail could not be sent), so it appears next to a success toast and would otherwise be read as part
 * of it and vanish with it after a few seconds — the one toast the user must not miss being the one
 * they had the least time for. Hence the explicit close button: without a timeout there has to be
 * something to click.
 */
const STICKY = { duration: Infinity, closeButton: true } as const;
