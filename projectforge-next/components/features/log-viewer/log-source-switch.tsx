"use client";

import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { updateSearchParams } from "@/lib/search-params";
import { cn } from "@/lib/utils";

export type LogSource = "memory" | "files";

/** The search parameter of the source (`files`; the memory without). */
export const SOURCE_PARAM = "source";

/** The problem whose occurrences in the log files are shown (see LogFileSearch). */
export const INCIDENT_PARAM = "incident";

/**
 * The chosen item of a toggle group of the log viewer filled: the default on state (a faint `bg-muted`) next to
 * the outlined off state reads as barely chosen.
 */
export const TOGGLE_ON_CLASS = cn(
  "data-[state=on]:border-primary data-[state=on]:bg-primary data-[state=on]:text-primary-foreground",
  "hover:data-[state=on]:bg-primary/90"
);

/**
 * The two sources of the admin log viewer: the last log events in memory (live, since the server's start) and
 * the log files of the last days (`?source=files`). Kept in the url of the page it is on (the system
 * dashboard's tab), so the back button returns to the previous one. Choosing one leaves a linked search or
 * problem (`?search=`, `?incident=`): the log files of a problem lead on to the free search.
 */
export function LogSourceSwitch({ value }: { value: LogSource }) {
  const t = useTranslations();
  const params = useSearchParams();
  const linked = params.has(INCIDENT_PARAM) || params.has("search");
  return (
    <ToggleGroup
      type="single"
      variant="outline"
      size="sm"
      value={value}
      // A click on the chosen item deselects it (empty value): it stays chosen, only a linked view is left.
      onValueChange={(chosen) => {
        const source = chosen || value;
        if (source === value && !linked) return;
        updateSearchParams(
          {
            [SOURCE_PARAM]: source === "files" ? "files" : null,
            [INCIDENT_PARAM]: null,
            search: null,
            threshold: null,
          },
          "push"
        );
      }}
    >
      <ToggleGroupItem value="memory" className={TOGGLE_ON_CLASS}>
        {t("system.admin.adminLogViewer.source.memory")}
      </ToggleGroupItem>
      <ToggleGroupItem value="files" className={TOGGLE_ON_CLASS}>
        {t("system.admin.adminLogViewer.source.files")}
      </ToggleGroupItem>
    </ToggleGroup>
  );
}
