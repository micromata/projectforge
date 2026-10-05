"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { startCreateJcrBackupZip, startMigrateJcrFiles } from "@/lib/rs/system";
import { toast } from "@/lib/toast";
import { JobProgress } from "./job-progress";
import type { SystemAdminData } from "./types";
import { useSystemJob } from "./use-system-job";

/**
 * Replacement of the JCR (Oak): shows the current store (`projectforge.files.store`), creates a
 * backup ZIP of the JCR and migrates the files out of Oak (DataTransfer files and, with
 * `projectforge.files.store=db`, all files). Both run as background jobs with an inline progress
 * (see [useSystemJob]).
 */
export function JcrCard({ data }: { data: SystemAdminData }) {
  const t = useTranslations();
  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("system.admin.group.title.jcr")}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <p className="text-sm">
          {data.allFilesInFileStore
            ? t("system.admin.jcr.store.db")
            : t("system.admin.jcr.store.jcr")}
        </p>
        <JcrJob
          labelKey="system.admin.button.createJcrBackupZip"
          infoKey="system.admin.jcr.createJcrBackupZip.info"
          startJob={startCreateJcrBackupZip}
        />
        <JcrJob
          labelKey="system.admin.button.migrateJcrFiles"
          infoKey="system.admin.jcr.migrateJcrFiles.info"
          confirmKey="system.admin.jcr.migrateJcrFiles.question"
          startJob={startMigrateJcrFiles}
        />
      </CardContent>
    </Card>
  );
}

function JcrJob({
  labelKey,
  infoKey,
  confirmKey,
  startJob,
}: {
  labelKey: string;
  infoKey: string;
  confirmKey?: string;
  startJob: () => Promise<{ jobId: number }>;
}) {
  const t = useTranslations();
  const { start, job, running } = useSystemJob<void>(startJob);
  const [confirmOpen, setConfirmOpen] = useState(false);

  async function onStart() {
    try {
      await start();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : String(err));
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <div>
        <Button
          size="sm"
          variant="outline"
          disabled={running}
          onClick={() => (confirmKey ? setConfirmOpen(true) : void onStart())}
        >
          {t(labelKey)}
        </Button>
      </div>
      <p className="text-xs text-muted-foreground">{t(infoKey)}</p>
      {job && <JobProgress job={job} />}
      {confirmKey && (
        <ConfirmDialog
          open={confirmOpen}
          onOpenChange={setConfirmOpen}
          title={t(labelKey)}
          description={t(confirmKey)}
          confirmLabel={t(labelKey)}
          onConfirm={() => void onStart()}
        />
      )}
    </div>
  );
}
