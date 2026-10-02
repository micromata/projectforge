"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon, PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FieldHint } from "@/components/shared/form/field-hint";
import type { Kost2Assignment } from "../contribution-margin-config";

/**
 * The kost2 assignments of the contribution margin: kost2 without an own project (first three parts of
 * their number) → the kost of the project they count for. Rows of the JSON value, not of a form array, so
 * an emptied row is simply posted empty and ignored by the backend.
 */
export function ContributionMarginKost2Rows({
  rows,
  onChange,
  onBlur,
}: {
  rows: Kost2Assignment[];
  onChange: (rows: Kost2Assignment[]) => void;
  onBlur: () => void;
}) {
  const t = useTranslations("fibu.auftrag.contributionMargin.config");
  const label = t("kost2Assignments._");
  const set = (index: number, patch: Partial<Kost2Assignment>) =>
    onChange(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  return (
    <div className="grid gap-2">
      <div className="flex items-center gap-1">
        <span className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
          {label}
        </span>
        <FieldHint hint={t("kost2Assignments.tooltip")} label={label} />
      </div>
      {rows.map((row, index) => (
        <div
          // The rows have no identity of their own; the index is what the JSON array has.
          key={index}
          className="grid grid-cols-[1fr_auto_1fr_auto] items-center gap-2"
        >
          <Input
            value={row.kost2 ?? ""}
            placeholder="6.000.10"
            aria-label={`${t("kost2")} ${index + 1}`}
            onChange={(e) => set(index, { kost2: e.target.value || null })}
            onBlur={onBlur}
          />
          <span className="text-muted-foreground" aria-hidden>
            →
          </span>
          <Input
            value={row.project ?? ""}
            placeholder="5.999.10"
            aria-label={`${t("project")} ${index + 1}`}
            onChange={(e) => set(index, { project: e.target.value || null })}
            onBlur={onBlur}
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={t("removeAssignment", { arg0: index + 1 })}
            onClick={() => onChange(rows.filter((_, i) => i !== index))}
          >
            <HugeiconsIcon icon={Delete02Icon} className="h-4 w-4" />
          </Button>
        </div>
      ))}
      <div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => onChange([...rows, { kost2: null, project: null }])}
        >
          <HugeiconsIcon icon={PlusSignIcon} className="h-4 w-4" />
          {t("addAssignment")}
        </Button>
      </div>
    </div>
  );
}
