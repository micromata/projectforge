"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { formatLogEntries } from "./format-log-entries";

/**
 * "Format log entries": pastes a one-line log snippet and pretty-prints it (see [formatLogEntries]).
 * Purely client-side — no backend call, as in the classic Wicket page.
 */
export function FormatLogEntriesCard() {
  const t = useTranslations();
  const [input, setInput] = useState("");
  const [formatted, setFormatted] = useState("");

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("system.admin.group.title.misc.logEntries")}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <Textarea
          value={input}
          onChange={(event) => setInput(event.target.value)}
          rows={4}
          aria-label={t(
            "system.admin.button.formatLogEntries.textarea.tooltip"
          )}
          placeholder={t(
            "system.admin.button.formatLogEntries.textarea.tooltip"
          )}
        />
        <div>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setFormatted(formatLogEntries(input))}
          >
            {t("system.admin.button.formatLogEntries._")}
          </Button>
        </div>
        {formatted && (
          <pre className="max-h-96 overflow-auto rounded-md border border-border bg-muted p-3 text-xs">
            {formatted}
          </pre>
        )}
      </CardContent>
    </Card>
  );
}
