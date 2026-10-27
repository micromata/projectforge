"use client";

import { useEffect, useRef } from "react";
import { useTranslations } from "next-intl";
import { CopyButton } from "@/components/shared/copy-button";
import { cn } from "@/lib/utils";
import type { SecretText } from "./use-secret-conversation";

/** The texts of a confidential conversation, the own ones to the right; the own ones taken by the partner are marked. */
export function SecretMessageList({
  texts,
  readCounter,
  partner,
}: {
  texts: SecretText[];
  readCounter: number;
  partner: string;
}) {
  const t = useTranslations();
  const end = useRef<HTMLDivElement>(null);
  useEffect(() => {
    end.current?.scrollIntoView?.({ block: "nearest" });
  }, [texts.length]);
  return (
    <div
      className="flex max-h-72 min-h-24 flex-col gap-2 overflow-y-auto rounded-md border p-2"
      data-testid="chat-secret-messages"
    >
      {texts.length === 0 && (
        <p className="text-sm text-muted-foreground">
          {t("chat.secret.empty", { arg0: partner })}
        </p>
      )}
      {texts.map((it) => (
        <div
          key={it.key}
          className={cn(
            "flex max-w-[85%] flex-col gap-0.5",
            it.mine ? "items-end self-end" : "items-start self-start"
          )}
          data-testid={it.mine ? "chat-secret-own" : "chat-secret-text"}
        >
          <div className="flex items-start gap-1">
            <p
              className={cn(
                "whitespace-pre-wrap break-all rounded-md px-2 py-1 font-mono text-sm",
                it.mine ? "bg-primary/10" : "bg-muted"
              )}
            >
              {it.text}
            </p>
            {!it.mine && (
              <CopyButton value={it.text} label={t("chat.secret.text")} />
            )}
          </div>
          {it.mine && it.counter <= readCounter && (
            <span className="text-xs text-muted-foreground">
              {t("chat.secret.read")}
            </span>
          )}
        </div>
      ))}
      <div ref={end} />
    </div>
  );
}
