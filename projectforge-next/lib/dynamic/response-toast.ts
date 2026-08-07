"use client";

import { toast } from "sonner";
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
  if (message.color === "danger") toast.error(text);
  else if (message.color === "warning") toast.warning(text);
  else if (message.color === "success") toast.success(text);
  else toast.info(text);
}
