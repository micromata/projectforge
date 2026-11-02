"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { GuardedDialog } from "@/components/shared/guarded-dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

/** Asks for the reason of reporting a message (optional) and whether its author is blocked as well. */
export function ReportMessageDialog({
  open,
  onOpenChange,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: (reason: string | null, block: boolean) => void;
}) {
  const t = useTranslations();
  const [reason, setReason] = useState("");
  const [block, setBlock] = useState(false);
  const reset = () => {
    setReason("");
    setBlock(false);
  };
  return (
    <GuardedDialog
      open={open}
      onOpenChange={(next) => {
        if (!next) reset();
        onOpenChange(next);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("chat.message.report.title")}</DialogTitle>
          <DialogDescription>
            {t("chat.message.report.description")}
          </DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="chatReportReason">
            {t("chat.message.report.reason")}
          </Label>
          <Textarea
            id="chatReportReason"
            value={reason}
            maxLength={1000}
            onChange={(event) => setReason(event.target.value)}
          />
        </div>
        <div className="flex items-center gap-2">
          <Checkbox
            id="chatReportBlock"
            checked={block}
            onCheckedChange={(checked) => setBlock(checked === true)}
          />
          <Label htmlFor="chatReportBlock">
            {t("chat.message.report.block")}
          </Label>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t("chat.message.cancel")}
          </Button>
          <Button
            onClick={() => {
              onConfirm(reason.trim() || null, block);
              reset();
            }}
          >
            {t("chat.message.report._")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </GuardedDialog>
  );
}
