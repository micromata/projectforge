"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { WorkHistoryIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { DataTransferAudit } from "@/components/shared/datatransfer-audit";

/**
 * The activities of the area, for a visitor of its external access who is logged in to ProjectForge with access
 * to the area (`DataTransferPublicNextRest.State.userInfoVisible`) — what the area's own page shows them in a
 * tab. Read from the internal endpoint, which checks that access itself.
 */
export function DataTransferPublicAuditButton({ id }: { id: number }) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const title = t("plugins.datatransfer.audit._");
  return (
    <>
      <Button
        variant="outline"
        size="sm"
        className="gap-1.5"
        onClick={() => setOpen(true)}
      >
        <HugeiconsIcon icon={WorkHistoryIcon} size={14} aria-hidden />
        {title}
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="flex max-h-[90vh] flex-col sm:max-w-4xl">
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
          </DialogHeader>
          {/* Mounted only while open, so the activities are read when asked for (see DataTransferAudit). */}
          {open && <DataTransferAudit id={id} />}
        </DialogContent>
      </Dialog>
    </>
  );
}
