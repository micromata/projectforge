"use client";

import { useEffect, useId, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  isAttentionSoundOn,
  playAttentionSound,
  setAttentionSoundOn,
} from "@/lib/attention-sound";
import { cn } from "@/lib/utils";

/**
 * How this browser draws attention to a confidential conversation offered (SecretOfferAlerts): the sound on or
 * off, and the permission for notifications of the system, asked only on a click (browsers refuse to ask
 * without one, and an unasked prompt is often denied for good).
 */
export function SecretAlertSettings({
  className,
  showTest,
}: {
  className?: string;
  /** A button to hear the sound, and a hint where the browser refuses notifications. */
  showTest?: boolean;
}) {
  const t = useTranslations();
  const soundId = useId();
  const [soundOn, setSoundOn] = useState(isAttentionSoundOn);
  const [permission, setPermission] = useState(() =>
    typeof Notification === "undefined" ? null : Notification.permission
  );
  // Changed in the settings of the browser while this is open: follow it where the Permissions API tells.
  useEffect(() => {
    let status: PermissionStatus | undefined;
    const update = () =>
      setPermission(
        typeof Notification === "undefined" ? null : Notification.permission
      );
    navigator.permissions
      ?.query({ name: "notifications" })
      .then((it) => {
        status = it;
        it.addEventListener("change", update);
      })
      .catch(() => {});
    return () => status?.removeEventListener("change", update);
  }, []);
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
      {showTest && (
        <Button
          type="button"
          size="sm"
          variant="outline"
          onClick={() => void playAttentionSound({ force: true })}
        >
          {t("chat.secret.alert.testSound")}
        </Button>
      )}
      {showTest && (
        <p>
          {t("chat.secret.alert.notificationsStatus")}{" "}
          <span className="font-medium">
            {permission === null
              ? t("chat.secret.alert.statusUnsupported")
              : permission === "granted"
                ? t("chat.secret.alert.statusGranted")
                : permission === "denied"
                  ? t("chat.secret.alert.statusDenied")
                  : t("chat.secret.alert.statusDefault")}
          </span>
        </p>
      )}
      {showTest && permission === "denied" && (
        <p className="text-muted-foreground">
          {t("chat.secret.alert.notificationsDenied")}
        </p>
      )}
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
