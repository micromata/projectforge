"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { fetchKost2ReallocationDefaultRules } from "@/lib/rs/kost2-reallocation";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { HintedLabel } from "./hinted-label";
import { PeriodFields } from "./period-fields";
import { RulesTable } from "./rules-table";
import type { RunDraft } from "./types";

const PROJECT_URL = "project/autosearch?search=:search";
const KOST2_URL = "cost2/autosearch?search=:search";

/**
 * Step 1: what the run reallocates — the target project, the
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
        <HintedLabel
          htmlFor="kr-description"
          label={t("description")}
          hint={t("fibu.kost2Reallocation.tooltip.description")}
        />
        <Input
          id="kr-description"
          value={draft.description}
          disabled={readOnly}
          onChange={(e) => set({ description: e.target.value })}
        />
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="flex flex-col gap-1">
          <HintedLabel
            label={t("fibu.kost2Reallocation.targetProject")}
            hint={t("fibu.kost2Reallocation.tooltip.targetProject")}
          />
          <EntityAutocomplete
            url={PROJECT_URL}
            value={draft.projekt}
            disabled={readOnly}
            aria-label={t("fibu.kost2Reallocation.targetProject")}
            onChange={(projekt) => set({ projekt })}
          />
        </div>
      </div>
      <PeriodFields draft={draft} readOnly={readOnly} onChange={set} />
      <div className="flex flex-col gap-2">
        <HintedLabel
          label={t("fibu.kost2Reallocation.sourceKost2")}
          hint={t("fibu.kost2Reallocation.tooltip.sourceKost2")}
        />
        <div className="flex items-start gap-2">
          <EntityMultiAutocomplete
            className="flex-1"
            url={KOST2_URL}
            value={sources}
            onChange={setSources}
            aria-label={t("fibu.kost2Reallocation.sourceKost2")}
            removeLabel={(entry) => `${t("delete")}: ${entry.displayName}`}
          />
          {/* A span: a disabled button raises no pointer events, so it couldn't show the hint itself. */}
          <HintTooltip text={t("fibu.kost2Reallocation.tooltip.defaultRules")}>
            <span>
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
            </span>
          </HintTooltip>
        </div>
        <RulesTable
          rules={draft.rules}
          readOnly={readOnly}
          onChange={(rules) => set({ rules })}
        />
      </div>
      <div className="flex flex-col gap-1">
        <HintedLabel
          htmlFor="kr-comment"
          label={t("comment")}
          hint={t("fibu.kost2Reallocation.tooltip.comment")}
        />
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
