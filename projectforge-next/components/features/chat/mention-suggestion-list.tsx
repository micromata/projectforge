"use client";

import { useTranslations } from "next-intl";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
} from "@/components/ui/popover";
import type { ChatRef } from "@/lib/rs/chat-types";
import { cn } from "@/lib/utils";
import type { MentionSuggestionState } from "./types";

/** The suggestion list of a mention, placed at the `@…` being typed. */
export function MentionSuggestionList({
  state,
  onChoose,
  onClose,
}: {
  state: MentionSuggestionState | null;
  onChoose: (item: ChatRef) => void;
  onClose: () => void;
}) {
  const t = useTranslations();
  const rect = state?.rect;
  return (
    <Popover open={!!state} onOpenChange={(open) => !open && onClose()}>
      <PopoverAnchor
        virtualRef={{
          current: { getBoundingClientRect: () => rect ?? new DOMRect() },
        }}
      />
      <PopoverContent
        align="start"
        side="top"
        className="w-64 gap-0 p-1"
        // The cursor stays in the editor: typing goes on while the list is open.
        onOpenAutoFocus={(event) => event.preventDefault()}
        onCloseAutoFocus={(event) => event.preventDefault()}
      >
        {state && state.items.length === 0 && (
          <div className="px-2 py-1.5 text-muted-foreground">
            {t("chat.mention.noResults")}
          </div>
        )}
        <ul role="listbox" aria-label={t("chat.message.placeholder")}>
          {state?.items.map((item, i) => (
            <li key={item.id} role="option" aria-selected={i === state.index}>
              <button
                type="button"
                className={cn(
                  "w-full rounded-md px-2 py-1.5 text-left hover:bg-muted",
                  i === state.index && "bg-muted"
                )}
                // Mouse down, not click: a click would blur the editor first and close the list.
                onMouseDown={(event) => {
                  event.preventDefault();
                  onChoose(item);
                }}
              >
                {item.displayName}
              </button>
            </li>
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  );
}
