"use client";

import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useFormatContext } from "@/hooks/use-format";
import { formatTimestampSeconds } from "@/lib/format";
import { EDITED, STORED, type CodeVersionOption } from "./use-code-versions";

/** Which version one side of the diff shows (see [useCodeVersions]). */
export function CodeVersionSelect({
  options,
  value,
  onChange,
  ariaLabel,
  storedLabel,
}: {
  options: CodeVersionOption[];
  value: string;
  onChange: (value: string) => void;
  ariaLabel: string;
  storedLabel?: string;
}) {
  const label = useVersionLabel(storedLabel);
  return (
    <Select value={value} onValueChange={onChange}>
      <SelectTrigger aria-label={ariaLabel} className="w-full sm:w-72">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {options.map((option) => (
          <SelectItem key={option.value} value={option.value}>
            {label(option)}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

function useVersionLabel(storedLabel?: string) {
  const t = useTranslations();
  const ctx = useFormatContext();
  return (option: CodeVersionOption): string => {
    if (option.value === EDITED) return t("codeEditor.diff.edited");
    if (option.value === STORED)
      return storedLabel ?? t("codeEditor.diff.stored");
    const timestamp = option.backup?.timestamp;
    return timestamp == null
      ? t("codeEditor.diff.backupWithoutDate")
      : t("codeEditor.diff.backup", {
          arg0: formatTimestampSeconds(timestamp, ctx),
        });
  };
}
