"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import { IhkSetupInstructions } from "./ihk-setup-instructions";
import type { IhkInit, IhkSettingsError } from "./types";

/** Why the settings could not be read, in one sentence. Keys spelled out, so the i18n scan finds them. */
function useReasonText(error: IhkSettingsError, init: IhkInit): string {
  const t = useTranslations();
  switch (error.reason) {
    case "notFound":
      return t("plugins.ihk.setup.reason.notFound", {
        arg0: init.firstname ?? "",
        arg1: init.lastname ?? "",
      });
    case "empty":
      return t("plugins.ihk.setup.reason.empty");
    case "parsing":
      return t("plugins.ihk.setup.reason.parsing", {
        arg0: error.detail ?? "",
      });
  }
}

function SetupMissing({
  init,
  error,
}: {
  init: IhkInit;
  error: IhkSettingsError;
}) {
  const t = useTranslations();
  const reason = useReasonText(error, init);
  return (
    <Alert>
      <AlertTitle>{t("plugins.ihk.setup.title")}</AlertTitle>
      <AlertDescription className="flex flex-col gap-3">
        <p className="font-medium text-destructive">{reason}</p>
        <IhkSetupInstructions init={init} />
      </AlertDescription>
    </Alert>
  );
}

/**
 * The apprentice's settings as read from the address, so a typo shows before the report is downloaded — or, while
 * they are missing or broken, why, and the setup instructions instead (the download is unavailable then).
 */
export function IhkSettingsStatus({ init }: { init: IhkInit }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  if (init.settingsError || !init.settings) {
    return (
      <SetupMissing
        init={init}
        error={init.settingsError ?? { reason: "empty" }}
      />
    );
  }
  const { ausbildungsbeginn, ausbildungsjahr, teamname } = init.settings;
  return (
    <Collapsible className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
        <span className="font-medium">{t("plugins.ihk.settings.title")}:</span>
        <span>
          <span className="text-muted-foreground">
            {t("plugins.ihk.settings.ausbildungsbeginn")}:
          </span>{" "}
          {formatDate(ausbildungsbeginn, ctx)}
        </span>
        <span>
          <span className="text-muted-foreground">
            {t("plugins.ihk.settings.ausbildungsjahr._")}:
          </span>{" "}
          {ausbildungsjahr > 0
            ? ausbildungsjahr
            : t("plugins.ihk.settings.ausbildungsjahr.auto")}
        </span>
        <span>
          <span className="text-muted-foreground">
            {t("plugins.ihk.settings.teamname")}:
          </span>{" "}
          {teamname || "–"}
        </span>
        <CollapsibleTrigger asChild>
          <Button
            type="button"
            variant="link"
            size="sm"
            className="group h-auto p-0"
          >
            {t("plugins.ihk.setup.show")}
            <HugeiconsIcon
              icon={ArrowDown01Icon}
              size={14}
              className="transition-transform group-data-[state=open]:rotate-180"
              aria-hidden
            />
          </Button>
        </CollapsibleTrigger>
      </div>
      <CollapsibleContent className="rounded-md border p-4">
        <IhkSetupInstructions init={init} />
      </CollapsibleContent>
    </Collapsible>
  );
}
