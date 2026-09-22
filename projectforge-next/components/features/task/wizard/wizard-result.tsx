"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { FormAlert } from "@/components/shared/form-alert";
import { SectionCard } from "@/components/shared/section-card";
import { TASK_TREE_ROUTE } from "@/components/shared/tasks/task-routes";
import type { TaskWizardResult as TaskWizardResultData } from "@/lib/rs/task";
import { STATUS_KEYS, STATUS_VARIANTS, groupEntries } from "./result-model";

export interface WizardResultProps {
  result: TaskWizardResultData;
  /** Puts the wizard back to its steps, with nothing picked. */
  onAgain: () => void;
}

/**
 * What the wizard did, once it has done it: per group the right it granted on the picked element and,
 * bundled into one line, the read access on that element's ancestors.
 *
 * The ancestors are bundled on purpose: they carry the same right for every group and their number
 * grows with the depth of the tree, so one line per group and role is what stays readable — the
 * numbers behind it say how many of them were new. A plain list, not a DataTable, as in
 * MassUpdateResultPanel.
 */
export function WizardResult({ result, onAgain }: WizardResultProps) {
  const t = useTranslations();
  const blocks = groupEntries(result.entries);

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-sm font-semibold">{t("task.wizard.result.title")}</h2>
      <FormAlert tone="success">
        {blocks.length === 0
          ? t("task.wizard.action.noactionRequired")
          : t("task.wizard.result.summary", {
              arg0: result.taskTitle ?? "",
              arg1: result.created,
              arg2: result.updated,
              arg3: result.unchanged,
            })}
      </FormAlert>
      {blocks.map((block) => (
        <SectionCard key={block.groupType} className="px-4 py-3">
          <ul className="flex flex-col gap-2 text-sm">
            {block.picked && (
              <ResultRow
                label={
                  <>
                    <span>{t(block.roleKey)}</span>
                    {block.groupName && (
                      <span className="font-medium"> {block.groupName}</span>
                    )}
                  </>
                }
                status={t(STATUS_KEYS[block.picked.status])}
                variant={STATUS_VARIANTS[block.picked.status]}
              />
            )}
            {block.ancestors.length > 0 && (
              <ResultRow
                label={t("task.wizard.result.ancestors", {
                  arg0: block.ancestors.length,
                })}
                status={block.ancestorCounts
                  .map(
                    ([status, count]) => `${count} ${t(STATUS_KEYS[status])}`
                  )
                  .join(", ")}
              />
            )}
          </ul>
        </SectionCard>
      ))}
      <div className="flex items-center gap-2">
        <Button type="button" variant="outline" onClick={onAgain}>
          {t("task.wizard.result.again")}
        </Button>
        <Button asChild>
          <Link href={TASK_TREE_ROUTE}>{t("task.wizard.result.toTree")}</Link>
        </Button>
      </div>
    </div>
  );
}

interface ResultRowProps {
  label: React.ReactNode;
  status: string;
  /** Without one the status is plain text: the bundled ancestors carry more than one of them. */
  variant?: "default" | "secondary" | "outline";
}

/** One granted right: the checkmark that says it holds, what it is, and what became of it. */
function ResultRow({ label, status, variant }: ResultRowProps) {
  return (
    <li className="flex items-center gap-2">
      <HugeiconsIcon
        icon={Tick02Icon}
        className="size-4 shrink-0 text-emerald-600"
        aria-hidden
      />
      <span className="grow">{label}</span>
      {variant ? (
        <Badge variant={variant}>{status}</Badge>
      ) : (
        <span className="text-xs text-muted-foreground">{status}</span>
      )}
    </li>
  );
}
