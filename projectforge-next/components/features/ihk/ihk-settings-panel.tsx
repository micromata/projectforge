"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { SectionCard } from "@/components/shared/section-card";
import { useFormatContext } from "@/hooks/use-format";
import { formatDate } from "@/lib/format";
import { IhkSettingsForm } from "./ihk-settings-form";
import type { IhkInit, IhkSettings } from "./types";

/** The stored settings in one line, so a typo shows before the report is downloaded. */
function SettingsSummary({ settings }: { settings: IhkSettings }) {
  const t = useTranslations();
  const ctx = useFormatContext();
  const entries = [
    [
      t("plugins.ihk.settings.ausbildungsbeginn"),
      settings.ausbildungsbeginn
        ? formatDate(settings.ausbildungsbeginn, ctx)
        : "–",
    ],
    [
      t("plugins.ihk.settings.ausbildungsjahr._"),
      settings.ausbildungsjahr > 0
        ? String(settings.ausbildungsjahr)
        : t("plugins.ihk.settings.ausbildungsjahr.auto"),
    ],
    [t("plugins.ihk.settings.teamname._"), settings.teamname || "–"],
  ] as const;
  return (
    <>
      {entries.map(([label, value]) => (
        <span key={label}>
          <span className="text-muted-foreground">{label}:</span> {value}
        </span>
      ))}
    </>
  );
}

/**
 * The apprentice's training settings: while they are missing, the form with a hint what to fill in (the page
 * offers no download then); once set, a summary line with a "Change" toggle, so a new team is entered quickly.
 * Right after the settings were taken over from the address (the former setup), a note says so.
 */
export function IhkSettingsPanel({ init }: { init: IhkInit }) {
  const t = useTranslations();
  const [editing, setEditing] = useState(false);
  const settings = init.settings;

  if (!settings) {
    return (
      <SectionCard className="flex flex-col gap-4">
        <h2 className="font-medium">{t("plugins.ihk.settings.title")}</h2>
        <p className="text-sm text-muted-foreground">
          {t("plugins.ihk.settings.notConfigured")}
        </p>
        <IhkSettingsForm />
      </SectionCard>
    );
  }
  return (
    <div className="flex flex-col gap-3">
      {init.migratedFromAddress && (
        <Alert>
          <AlertTitle>{t("plugins.ihk.settings.title")}</AlertTitle>
          <AlertDescription>
            {t("plugins.ihk.settings.migrated")}
          </AlertDescription>
        </Alert>
      )}
      {editing ? (
        <SectionCard className="flex flex-col gap-4">
          <h2 className="font-medium">{t("plugins.ihk.settings.title")}</h2>
          <IhkSettingsForm
            initial={settings}
            onDone={() => setEditing(false)}
          />
        </SectionCard>
      ) : (
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
          <span className="font-medium">
            {t("plugins.ihk.settings.title")}:
          </span>
          <SettingsSummary settings={settings} />
          <Button
            type="button"
            variant="link"
            size="sm"
            className="h-auto p-0"
            onClick={() => setEditing(true)}
          >
            {t("plugins.ihk.settings.edit")}
          </Button>
        </div>
      )}
    </div>
  );
}
