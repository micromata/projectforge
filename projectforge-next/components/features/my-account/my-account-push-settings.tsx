"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { RsError } from "@/lib/rs/client";
import {
  fetchPushSettings,
  PUSH_SETTINGS_QUERY_KEY,
  savePushSettings,
  type PushSettings,
} from "@/lib/rs/push-settings";
import { toast } from "@/lib/toast";

/**
 * Whether the pushes to the user's apps show the content of a notification: saved at once, apart from the form (the
 * same setting as in the app). Hidden if the server keeps the content out of every push.
 */
export function MyAccountPushSettings() {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [busy, setBusy] = useState(false);
  const query = useQuery({
    queryKey: PUSH_SETTINGS_QUERY_KEY,
    queryFn: ({ signal }) => fetchPushSettings(signal),
  });
  const settings = query.data;
  if (!settings?.available) return null;

  const change = async (content: boolean) => {
    setBusy(true);
    try {
      const saved = await savePushSettings(content);
      queryClient.setQueryData<PushSettings>(PUSH_SETTINGS_QUERY_KEY, saved);
    } catch (error) {
      toast.error(error instanceof RsError ? error.message : String(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex items-start gap-2">
      <Switch
        id="my-account-push-content"
        data-testid="my-account-push-content"
        checked={settings.content}
        disabled={busy || settings.writeAccess === false}
        onCheckedChange={(checked) => void change(checked)}
      />
      <div className="flex flex-col gap-0.5">
        <Label htmlFor="my-account-push-content">
          {t("notification.push.content")}
        </Label>
        <p className="text-sm text-muted-foreground">
          {t("notification.push.contentInfo")}
        </p>
      </div>
    </div>
  );
}
