"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import {
  previewNotificationRule,
  testNotificationRuleToMe,
  triggerNotificationRule,
  type NotificationPreview,
} from "@/lib/rs/notification";
import { toast } from "@/lib/toast";
import { RulePreviewDialog } from "./rule-preview-dialog";

const reportError = (error: unknown) =>
  toast.error(error instanceof Error ? error.message : String(error));

/**
 * Preview and test work on the values of the form, saved or not; trigger runs the saved rule now,
 * regardless of its schedule (recipients already notified for the period are not notified again).
 */
export function RuleActions({ id }: { id: number | null }) {
  const t = useTranslations("notification");
  const form = useEntityEditForm();
  const [preview, setPreview] = useState<NotificationPreview | null>(null);
  const [confirmTrigger, setConfirmTrigger] = useState(false);
  const values = () => form.state.values as object;

  const previewMutation = useMutation({
    mutationFn: () => previewNotificationRule(values()),
    onSuccess: setPreview,
    onError: reportError,
  });
  const testMutation = useMutation({
    mutationFn: () => testNotificationRuleToMe(values()),
    onSuccess: (result) => {
      if (result.delivered.length > 0) {
        const channels = result.delivered.map((c) => t(`channels.${c}`));
        toast.success(t("testDelivered", { arg0: channels.join(", ") }));
      }
      const failed = Object.entries(result.failed);
      if (failed.length > 0) {
        toast.error(
          t("testFailed", {
            arg0: failed
              .map(([c, e]) => `${t(`channels.${c}`)}: ${e}`)
              .join("; "),
          })
        );
      }
    },
    onError: reportError,
  });
  const triggerMutation = useMutation({
    mutationFn: (ruleId: number) => triggerNotificationRule(ruleId),
    onSuccess: (result) => {
      toast.success(
        t("triggerResult", { arg0: result.created, arg1: result.recipients })
      );
      if (result.errors > 0) {
        toast.error(
          t("triggerErrors", {
            arg0: result.errors,
            arg1: result.lastError ?? "",
          })
        );
      }
    },
    onError: reportError,
  });

  return (
    <div className="flex flex-wrap gap-2">
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={previewMutation.isPending}
        onClick={() => previewMutation.mutate()}
      >
        {t("action.preview")}
      </Button>
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={testMutation.isPending}
        onClick={() => testMutation.mutate()}
      >
        {t("action.testToMe")}
      </Button>
      {id != null && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={triggerMutation.isPending}
          onClick={() => setConfirmTrigger(true)}
        >
          {t("action.trigger")}
        </Button>
      )}
      <RulePreviewDialog
        preview={preview}
        onOpenChange={(open) => !open && setPreview(null)}
      />
      <ConfirmDialog
        open={confirmTrigger}
        onOpenChange={setConfirmTrigger}
        title={t("action.trigger")}
        description={t("action.triggerConfirm")}
        confirmLabel={t("action.trigger")}
        onConfirm={() => id != null && triggerMutation.mutate(id)}
      />
    </div>
  );
}
