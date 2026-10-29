"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { EntityAutocomplete } from "@/components/shared/entity-autocomplete";
import { EntityMultiAutocomplete } from "@/components/shared/entity-multi-autocomplete";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { fetchKost2ReallocationDefaultRules } from "@/lib/rs/kost2-reallocation";
import { FormAlert } from "@/components/shared/form-alert";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { HintedLabel } from "./hinted-label";
import { PeriodFields } from "./period-fields";
import { RulesTable } from "./rules-table";
import type { RunDraft, RunSources } from "./types";

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
  sources: runSources,
  onSourcesChange,
}: {
  draft: RunDraft;
  onChange: (draft: RunDraft) => void;
  readOnly: boolean;
  sources: RunSources;
  onSourcesChange: (sources: RunSources) => void;
}) {
  const t = useTranslations();
  const sourceProjects = runSources.projects;
  // Not chosen yet (a stored run opened): the source kost2 of its rules.
  const sources =
    runSources.kost2 ??
    draft.rules.flatMap((r) => (r.sourceKost2 ? [r.sourceKost2] : []));
  const set = (patch: Partial<RunDraft>) => onChange({ ...draft, ...patch });
  const [error, setError] = useState<string | null>(null);
  const fill = useMutation({
    mutationFn: () =>
      fetchKost2ReallocationDefaultRules(
        draft.projekt!.id,
        sources.map((s) => s.id),
        sourceProjects.map((p) => p.id)
      ),
    onSuccess: (rules) => {
      setError(null);
      set({ rules });
    },
    onError: (e) => setError(e instanceof Error ? e.message : String(e)),
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
        {/* The sources only drive the default rules: an applied or reverted run has nothing to derive. */}
        {!readOnly && (
          <>
            <HintedLabel
              label={t("fibu.kost2Reallocation.sourceProject")}
              hint={t("fibu.kost2Reallocation.tooltip.sourceProject")}
            />
            <EntityMultiAutocomplete
              url={PROJECT_URL}
              value={sourceProjects}
              onChange={(projects) =>
                onSourcesChange({ projects, kost2: sources })
              }
              aria-label={t("fibu.kost2Reallocation.sourceProject")}
              removeLabel={(entry) => `${t("delete")}: ${entry.displayName}`}
            />
            <HintedLabel
              label={t("fibu.kost2Reallocation.sourceKost2")}
              hint={t("fibu.kost2Reallocation.tooltip.sourceKost2")}
            />
            <div className="flex items-start gap-2">
              <EntityMultiAutocomplete
                className="flex-1"
                url={KOST2_URL}
                value={sources}
                onChange={(kost2) =>
                  onSourcesChange({ projects: sourceProjects, kost2 })
                }
                aria-label={t("fibu.kost2Reallocation.sourceKost2")}
                removeLabel={(entry) => `${t("delete")}: ${entry.displayName}`}
              />
              {/* A span: a disabled button raises no pointer events, so it couldn't show the hint itself. */}
              <HintTooltip
                text={t("fibu.kost2Reallocation.tooltip.defaultRules")}
              >
                <span>
                  <Button
                    type="button"
                    variant="outline"
                    disabled={
                      !draft.projekt ||
                      (sources.length === 0 && sourceProjects.length === 0) ||
                      fill.isPending
                    }
                    onClick={() => fill.mutate()}
                  >
                    {t("fibu.kost2Reallocation.defaultRules")}
                  </Button>
                </span>
              </HintTooltip>
            </div>
            {error && <FormAlert tone="error">{error}</FormAlert>}
          </>
        )}
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
