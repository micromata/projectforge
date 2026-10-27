"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { TaskSelect } from "@/components/shared/tasks/task-select";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { fetchKost2ReallocationDefaultRules } from "@/lib/rs/kost2-reallocation";
import { PeriodFields } from "./period-fields";
import { RulesTable } from "./rules-table";
import type { RunDraft } from "./types";

const PROJECT_URL = "project/autosearch?search=:search";
const KOST2_URL = "cost2/autosearch?search=:search";

/**
 * Step 1: what the run reallocates — the target project (and the task of time sheets selected by hand), the
 * period, and the rules A → B for the other records. The default rules map each source cost unit to the
 * target project's cost unit of the same type.
 */
export function RunDefinitionStep({
  draft,
  onChange,
  readOnly,
}: {
  draft: RunDraft;
  onChange: (draft: RunDraft) => void;
  readOnly: boolean;
}) {
  const t = useTranslations();
  const [sources, setSources] = useState<EntityRef[]>(
    draft.rules.flatMap((r) => (r.sourceKost2 ? [r.sourceKost2] : []))
  );
  const set = (patch: Partial<RunDraft>) => onChange({ ...draft, ...patch });
  const fill = useMutation({
    mutationFn: () =>
      fetchKost2ReallocationDefaultRules(
        draft.projekt!.id,
        sources.map((s) => s.id)
      ),
    onSuccess: (rules) => set({ rules }),
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <Label htmlFor="kr-description">{t("description")}</Label>
        <Input
          id="kr-description"
          value={draft.description}
          disabled={readOnly}
          onChange={(e) => set({ description: e.target.value })}
        />
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="flex flex-col gap-1">
          <Label>{t("fibu.kost2Reallocation.targetProject")}</Label>
          <EntityAutocomplete
            url={PROJECT_URL}
            value={draft.projekt}
            disabled={readOnly}
            aria-label={t("fibu.kost2Reallocation.targetProject")}
            onChange={(projekt) => set({ projekt })}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label>{t("fibu.kost2Reallocation.targetTask")}</Label>
          <TaskSelect
            value={draft.taskId}
            onChange={(taskId) => set({ taskId })}
            aria-label={t("fibu.kost2Reallocation.targetTask")}
          />
        </div>
      </div>
      <PeriodFields draft={draft} readOnly={readOnly} onChange={set} />
      <div className="flex flex-col gap-2">
        <Label>{t("fibu.kost2Reallocation.sourceKost2")}</Label>
        <div className="flex items-start gap-2">
          <EntityMultiAutocomplete
            className="flex-1"
            url={KOST2_URL}
            value={sources}
            onChange={setSources}
            aria-label={t("fibu.kost2Reallocation.sourceKost2")}
            removeLabel={(entry) => `${t("delete")}: ${entry.displayName}`}
          />
          <Button
            type="button"
            variant="outline"
            disabled={
              readOnly ||
              !draft.projekt ||
              sources.length === 0 ||
              fill.isPending
            }
            onClick={() => fill.mutate()}
          >
            {t("fibu.kost2Reallocation.defaultRules")}
          </Button>
        </div>
        <RulesTable
          rules={draft.rules}
          readOnly={readOnly}
          onChange={(rules) => set({ rules })}
        />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="kr-comment">{t("comment")}</Label>
        <Textarea
          id="kr-comment"
          rows={2}
          value={draft.comment}
          disabled={readOnly}
          onChange={(e) => set({ comment: e.target.value })}
        />
      </div>
    </div>
  );
}
