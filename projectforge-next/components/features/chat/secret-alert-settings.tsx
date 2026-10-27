"use client";

import { useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { isAttentionSoundOn, setAttentionSoundOn } from "@/lib/attention-sound";
import { cn } from "@/lib/utils";

/**
 * How this browser draws attention to a confidential conversation offered (SecretOfferAlerts): the sound on or
 * off, and the permission for notifications of the system, asked only on a click (browsers refuse to ask
 * without one, and an unasked prompt is often denied for good).
 */
export function SecretAlertSettings({ className }: { className?: string }) {
  const t = useTranslations();
  const soundId = useId();
  const [soundOn, setSoundOn] = useState(isAttentionSoundOn);
  const [permission, setPermission] = useState(() =>
    typeof Notification === "undefined" ? null : Notification.permission
  );
  return (
    <div
      className={cn(
        "flex flex-wrap items-center gap-x-4 gap-y-2 text-sm",
        className
      )}
    >
      <div className="flex items-center gap-2">
        <Switch
          id={soundId}
          size="sm"
          checked={soundOn}
          onCheckedChange={(on) => {
            setAttentionSoundOn(on);
            setSoundOn(on);
          }}
        />
        <Label htmlFor={soundId} className="font-normal">
          {t("chat.secret.alert.sound")}
        </Label>
      </div>
      {permission === "default" && (
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() =>
            void Notification.requestPermission().then(setPermission)
          }
        >
          {t("chat.secret.alert.allowNotifications")}
        </Button>
      )}
    </div>
  );
}
