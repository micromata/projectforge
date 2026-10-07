"use client";

import { useState, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Textarea } from "@/components/ui/textarea";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { FieldHint } from "@/components/shared/form/field-hint";
import { cn } from "@/lib/utils";
import { splitEntries } from "../lanes-and-planes-config";

type Mode = "list" | "text";

/**
 * A list setting of the Lanes & Planes parameter, shown either as its list input ([list]) or as a text
 * area with one entry per line, to paste or copy the whole list (CSV or a spreadsheet column). Both edit the
 * same entries; the mode is only a view and isn't stored.
 */
export function ListOrTextSetting({
  label,
  hint,
  invalid,
  entries,
  onChange,
  onBlur,
  placeholder,
  list,
}: {
  label: string;
  hint: string;
  /** The backend reported an error about this setting: its label is marked. */
  invalid?: boolean;
  entries: string[];
  onChange: (entries: string[]) => void;
  onBlur: () => void;
  placeholder?: string;
  list: ReactNode;
}) {
  const t = useTranslations("lanesAndPlanes.config");
  const [mode, setMode] = useState<Mode>("list");
  // The text as typed while the area has the focus: rebuilt from the entries, a just typed separator or
  // blank line would vanish under the cursor.
  const [draft, setDraft] = useState<string | null>(null);
  return (
    <div className="grid content-start gap-2">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1">
          <span
            className={cn(
              "text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground",
              invalid && "text-destructive"
            )}
          >
            {label}
          </span>
          <FieldHint hint={hint} label={label} />
        </div>
        <ToggleGroup
          type="single"
          variant="outline"
          size="sm"
          spacing={0}
          value={mode}
          // Radix reports "" when the active item is clicked again: keep the mode then.
          onValueChange={(value) => value && setMode(value as Mode)}
        >
          <ToggleGroupItem value="list" className="h-6 px-2 text-xs">
            {t("mode.list")}
          </ToggleGroupItem>
          <ToggleGroupItem value="text" className="h-6 px-2 text-xs">
            {t("mode.text")}
          </ToggleGroupItem>
        </ToggleGroup>
      </div>
      {mode === "list" ? (
        list
      ) : (
        <div className="grid gap-1">
          <Textarea
            value={draft ?? entries.join("\n")}
            placeholder={placeholder}
            aria-label={label}
            aria-invalid={invalid || undefined}
            rows={6}
            className="min-h-32 font-mono"
            onFocus={() => setDraft(entries.join("\n"))}
            onChange={(e) => {
              setDraft(e.target.value);
              onChange(splitEntries(e.target.value));
            }}
            onBlur={() => {
              setDraft(null);
              onBlur();
            }}
          />
          <p className="text-xs text-muted-foreground">{t("textHint")}</p>
        </div>
      )}
    </div>
  );
}
