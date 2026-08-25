"use client";

import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { ImportColumnInfo } from "./import-column-info";
import { ImportControls } from "./import-controls";
import { ImportDropStep } from "./import-drop-step";
import { ImportPreviewTable } from "./import-preview-table";
import { ImportStatisticsLine } from "./import-statistics-line";
import { selectableIds } from "./import-model";
import { useImport } from "./use-import";
import type { ImportConfig } from "./import-types";

interface Props {
  config: ImportConfig;
}

/**
 * The whole import flow of one entity, driven by its [ImportConfig]: while nothing is stashed it is the
 * drop step; once a file is uploaded it is the preview — the statistics line, the config-driven table and
 * the reconcile/commit/cancel bar. Generic by construction, so the incoming-invoice route and the later
 * address/banking routes are each a one-line consumer that hands it a config.
 */
export function ImportFeature({ config }: Props) {
  const t = useTranslations();
  const imp = useImport(config);
  const view = imp.view;

  if (!imp.hasStorage) {
    return (
      <SectionCard className="flex flex-col gap-3">
        <p className="text-sm text-muted-foreground">{t(config.titleKey)}</p>
        <ImportDropStep
          config={config}
          onFile={(file) => imp.upload.mutate(file)}
          uploadProgress={imp.uploadProgress}
        />
      </SectionCard>
    );
  }

  const meta = view?.meta ?? {};
  const totalSelectable = selectableIds(
    view?.entries ?? [],
    config.selectableStatuses
  ).length;

  return (
    <div className="flex flex-col gap-3">
      <SectionCard className="flex flex-col gap-3">
        <ImportStatisticsLine info={view?.info} />
        <ImportControls
          hasBeenReconciled={view?.hasBeenReconciled ?? false}
          selectedCount={imp.selectedIds.length}
          totalSelectable={totalSelectable}
          onReconcile={() => imp.reconcile.mutate()}
          onCommit={() => imp.commit.mutate(imp.selectedIds)}
          onCancel={() => imp.cancel.mutate()}
          onSelectAll={imp.selectAll}
          onDeselectAll={imp.clearSelection}
          isReconciling={imp.reconcile.isPending}
          isCommitting={imp.commit.isPending}
          isCancelling={imp.cancel.isPending}
        />
        <div className="flex max-h-[70vh] flex-col">
          <ImportPreviewTable
            config={config}
            entries={view?.entries ?? []}
            meta={meta}
            selection={imp.selection}
            onSelectionChange={imp.setSelection}
            isFetching={imp.query.isFetching || imp.reconcile.isPending}
          />
        </div>
      </SectionCard>
      {/* The column reference sits at the foot, as it does on the classic page. */}
      <ImportColumnInfo info={view?.info} />
    </div>
  );
}
