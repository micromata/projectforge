"use client";

import { useCallback, type ComponentProps } from "react";
import { useTranslations } from "next-intl";
import { CUSTOM_EMOJIS } from "@/components/shared/emoji/custom-emoji";
import { decorateEmoji } from "@/components/shared/emoji/decorate-emoji";
import { RichText } from "@/components/shared/rich-text";

/**
 * The description of a room: a rich text as written with DESCRIPTION_FEATURES, i.e. with code, and with the
 * emoji drawn as in a message.
 */
export function ChatDescription(
  props: Omit<ComponentProps<typeof RichText>, "code" | "decorate">
) {
  const t = useTranslations();
  const decorate = useCallback(
    (html: string) => decorateEmoji(html, (code) => t(CUSTOM_EMOJIS[code])),
    [t]
  );
  return <RichText {...props} code decorate={decorate} />;
}
