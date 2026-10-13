"use client";

import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { RichText } from "@/components/shared/rich-text";
import type { NotificationPreview } from "@/lib/rs/notification";

/** Who the rule would notify today, and its texts rendered with the values of the logged-in user. */
export function RulePreviewDialog({
  preview,
  onOpenChange,
}: {
  preview: NotificationPreview | null;
  onOpenChange: (open: boolean) => void;
}) {
  const t = useTranslations("notification");
  return (
    <Dialog open={preview != null} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t("action.preview")}</DialogTitle>
        </DialogHeader>
        {preview && (
          <div className="space-y-4 text-sm">
            {preview.recipientCount === 0 ? (
              <p>{t("preview.none")}</p>
            ) : (
              <div className="space-y-1">
                <p className="font-medium">
                  {t("preview.recipients", { arg0: preview.recipientCount })}
                  {preview.alreadyNotified > 0 &&
                    ` — ${t("preview.alreadyNotified", { arg0: preview.alreadyNotified })}`}
                </p>
                <p className="text-muted-foreground">
                  {preview.recipients.join(", ")}
                  {preview.recipients.length < preview.recipientCount && ", …"}
                </p>
              </div>
            )}
            {preview.periodKey && (
              <p>
                {t("preview.period")}: {preview.periodKey}
              </p>
            )}
            <div className="space-y-2 rounded-md border p-3">
              <p className="text-xs text-muted-foreground">
                {t("preview.sample")}
              </p>
              <p className="font-medium">{preview.subject}</p>
              <RichText html={preview.text} />
            </div>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
