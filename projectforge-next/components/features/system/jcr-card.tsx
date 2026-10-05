"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import {
  startCreateJcrBackupZip,
  startMigrateJcrFileSystemPaths,
} from "@/lib/rs/system";
import { toast } from "@/lib/toast";
import { JobProgress } from "./job-progress";
import { useSystemJob } from "./use-system-job";

/**
 * Preparation for the replacement of the JCR (Oak): a backup ZIP of the entity files, which the
 * release without Oak imports, and moving the DataTransfer files out of Oak into the file system.
 * Both run as background jobs with an inline progress (see [useSystemJob]).
 */
export function JcrCard() {
  const t = useTranslations();
  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("system.admin.group.title.jcr")}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        <JcrJob
          labelKey="system.admin.button.createJcrBackupZip"
          infoKey="system.admin.jcr.createJcrBackupZip.info"
          startJob={startCreateJcrBackupZip}
        />
        <JcrJob
          labelKey="system.admin.button.migrateJcrFileSystemPaths"
          infoKey="system.admin.jcr.migrateJcrFileSystemPaths.info"
          confirmKey="system.admin.jcr.migrateJcrFileSystemPaths.question"
          startJob={startMigrateJcrFileSystemPaths}
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
