"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { cn } from "@/lib/utils";

export type LogSource = "memory" | "files";

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
 * the log files of the last days (`?source=files`).
 */
export function LogSourceSwitch({ value }: { value: LogSource }) {
  const t = useTranslations();
  const router = useRouter();
  return (
    <ToggleGroup
      type="single"
      variant="outline"
      size="sm"
      value={value}
      onValueChange={(source) => {
        if (!source || source === value) return;
        // The router prepends the base path /next itself.
        router.replace(
          source === "files"
            ? "/adminLogViewer?source=files"
            : "/adminLogViewer"
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
