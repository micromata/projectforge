"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { DateInput } from "@/components/shared/date-input";
import { Select, SelectTrigger } from "@/components/shared/copyable-select";
import { Button } from "@/components/ui/button";
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { SelectContent, SelectItem, SelectValue } from "@/components/ui/select";
import { RsError } from "@/lib/rs/client";
import { saveIhkSettings } from "@/lib/rs/ihk";
import { toast } from "@/lib/toast";
import {
  IHK_AUSBILDUNGSJAHR_AUTO,
  IHK_AUSBILDUNGSJAHRE,
  type IhkInit,
  type IhkSettings,
} from "./types";

const AUTO = "auto";

/**
 * The apprentice's training settings, printed on every report. Plain `useState`, like the other standalone forms
 * of this app: three fields, the only rule ("a training start is given") is enforced by the disabled Save, the
 * range of the year by the select — the server's check (406) is a guard only.
 *
 * Saving replaces the settings of the cached `init`, so the page offers the download at once.
 */
export function IhkSettingsForm({
  initial,
  onDone,
}: {
  initial?: IhkSettings;
  /** Called after a successful save or on Cancel; Cancel is shown only with it. */
  onDone?: () => void;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const [ausbildungsbeginn, setAusbildungsbeginn] = useState(
    initial?.ausbildungsbeginn ?? null
  );
  const [ausbildungsjahr, setAusbildungsjahr] = useState(
    initial?.ausbildungsjahr ?? IHK_AUSBILDUNGSJAHR_AUTO
  );
  const [teamname, setTeamname] = useState(initial?.teamname ?? "");

  const save = useMutation({
    mutationFn: () =>
      saveIhkSettings({
        ausbildungsbeginn: ausbildungsbeginn ?? undefined,
        ausbildungsjahr,
        teamname,
      }),
    onSuccess: (settings) => {
      queryClient.setQueryData<IhkInit>(["ihk", "init"], (old) =>
        old ? { ...old, settings, migratedFromAddress: false } : old
      );
      toast.success(t("plugins.ihk.settings.saved"));
      onDone?.();
    },
    onError: (error) =>
      toast.error(error instanceof RsError ? error.message : String(error)),
  });

  return (
    <FieldGroup className="max-w-md">
      <Field>
        <FieldLabel htmlFor="ihk-ausbildungsbeginn">
          {t("plugins.ihk.settings.ausbildungsbeginn")}
        </FieldLabel>
        <DateInput
          id="ihk-ausbildungsbeginn"
          value={ausbildungsbeginn}
          onChange={setAusbildungsbeginn}
          required
        />
      </Field>
      <Field>
        <FieldLabel htmlFor="ihk-ausbildungsjahr">
          {t("plugins.ihk.settings.ausbildungsjahr._")}
        </FieldLabel>
        <Select
          value={ausbildungsjahr > 0 ? String(ausbildungsjahr) : AUTO}
          onValueChange={(value) =>
            setAusbildungsjahr(
              value === AUTO ? IHK_AUSBILDUNGSJAHR_AUTO : Number(value)
            )
          }
        >
          <SelectTrigger id="ihk-ausbildungsjahr" className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={AUTO}>
              {t("plugins.ihk.settings.ausbildungsjahr.auto")}
            </SelectItem>
            {IHK_AUSBILDUNGSJAHRE.map((year) => (
              <SelectItem key={year} value={String(year)}>
                {year}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <FieldDescription>
          {t("plugins.ihk.settings.ausbildungsjahr.hint")}
        </FieldDescription>
      </Field>
      <Field>
        <FieldLabel htmlFor="ihk-teamname">
          {t("plugins.ihk.settings.teamname._")}
        </FieldLabel>
        <Input
          id="ihk-teamname"
          value={teamname}
          onChange={(event) => setTeamname(event.target.value)}
        />
        <FieldDescription>
          {t("plugins.ihk.settings.teamname.hint")}
        </FieldDescription>
      </Field>
      <div className="flex items-center gap-3">
        {onDone && (
          <Button type="button" variant="outline" onClick={onDone}>
            {t("cancel")}
          </Button>
        )}
        <Button
          type="button"
          disabled={!ausbildungsbeginn || save.isPending}
          onClick={() => save.mutate()}
        >
          {t("save")}
        </Button>
      </div>
    </FieldGroup>
  );
}
