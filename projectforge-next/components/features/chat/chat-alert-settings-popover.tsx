"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Settings02Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { RingLogView } from "./ring-log-view";
import { SecretAlertSettings } from "./secret-alert-settings";

/** The gear of the chat: sound and notifications of the browser, and the log of the last signals. */
export function ChatAlertSettingsPopover() {
  const t = useTranslations();
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          size="icon"
          aria-label={t("chat.secret.alert.settings")}
          title={t("chat.secret.alert.settings")}
        >
          <HugeiconsIcon icon={Settings02Icon} size={16} />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-80">
        <h2 className="text-sm font-semibold">
          {t("chat.secret.alert.settings")}
        </h2>
        <SecretAlertSettings showTest className="flex-col items-start" />
        <RingLogView />
      </PopoverContent>
    </Popover>
  );
}
