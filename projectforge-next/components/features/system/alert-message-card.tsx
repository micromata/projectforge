"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "@/lib/toast";
import { clearAlertMessage, setAlertMessage } from "@/lib/rs/system";
import type { SystemAdminData } from "./types";

/**
 * The system alert message shown red on every page (see SystemAlertBanner) — set or cleared here. Not
 * persisted: it lives in memory until cleared or ProjectForge restarts. Setting it invalidates
 * `userStatus`, whose refresh is what feeds the banner, so the change shows at once.
 */
export function AlertMessageCard({ data }: { data: SystemAdminData }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [message, setMessage] = useState(data.alertMessage ?? "");

  const refreshBanner = () =>
    void queryClient.invalidateQueries({ queryKey: ["userStatus"] });

  const setMutation = useMutation({
    mutationFn: () => setAlertMessage(message),
    onSuccess: (res) => {
      toast.success(res.message);
      refreshBanner();
    },
    onError: (err) => toast.error(errorMessage(err)),
  });

  const clearMutation = useMutation({
    mutationFn: clearAlertMessage,
    onSuccess: (res) => {
      setMessage("");
      toast.success(res.message);
      refreshBanner();
    },
    onError: (err) => toast.error(errorMessage(err)),
  });

  const busy = setMutation.isPending || clearMutation.isPending;

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("system.admin.group.title.alertMessage")}</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <Textarea
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          rows={3}
          maxLength={1000}
          aria-label={t("system.admin.group.title.alertMessage")}
        />
        <div className="flex items-center gap-3">
          <Button
            size="sm"
            disabled={busy || message.trim().length === 0}
            onClick={() => setMutation.mutate()}
          >
            {t("system.admin.button.setAlertMessage")}
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={busy}
            onClick={() => clearMutation.mutate()}
          >
            {t("system.admin.button.clearAlertMessage")}
          </Button>
        </div>
        {/* The copy&paste maintenance-notice sample, as offered by the classic page. */}
        <div className="flex flex-col gap-1">
          <p className="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
            {t("system.admin.alertMessage.copyAndPaste.title")}
          </p>
          <p className="rounded-md border border-border bg-muted p-3 text-xs">
            {data.alertMessageSample}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}

function errorMessage(err: unknown): string {
  return err instanceof Error ? err.message : String(err);
}
