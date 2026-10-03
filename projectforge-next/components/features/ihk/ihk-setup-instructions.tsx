"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";
import type { IhkInit } from "./types";

/**
 * The JSON the address comment has to hold. Not translated: the property names are what the backend parses
 * (`IHKCommentObject`), the values are placeholders the apprentice replaces.
 */
const EXAMPLE_JSON = `{
  "ausbildungsbeginn": "2024-09-01",
  "ausbildungsjahr": -1,
  "teamname": "Projekt xyz"
}`;

/**
 * What an apprentice has to do before the page works: the settings are a JSON object in the comment of the
 * user's own address in the contacts, and the address is found by first and last name only. The Wicket page
 * merely said "ask the other apprentices", so this spells the setup out step by step, with a link to the address.
 */
export function IhkSetupInstructions({ init }: { init: IhkInit }) {
  const t = useTranslations();
  const fields = [
    ["ausbildungsbeginn", t("plugins.ihk.setup.field.ausbildungsbeginn")],
    ["ausbildungsjahr", t("plugins.ihk.setup.field.ausbildungsjahr")],
    ["teamname", t("plugins.ihk.setup.field.teamname")],
  ] as const;
  return (
    <div className="flex flex-col gap-3 text-sm">
      <p>{t("plugins.ihk.setup.intro")}</p>
      <p className="font-medium">{t("plugins.ihk.setup.steps")}</p>
      <ol className="flex list-decimal flex-col gap-2 pl-5">
        <li>
          {t("plugins.ihk.setup.step1", {
            arg0: init.firstname ?? "",
            arg1: init.lastname ?? "",
          })}
          <div className="mt-1">
            <Button asChild size="sm" variant="outline">
              <a href={toAbsoluteUrl(resolveMenuUrl(init.addressUrl))}>
                {init.settingsError?.reason === "notFound"
                  ? t("plugins.ihk.setup.address.new")
                  : t("plugins.ihk.setup.address.edit")}
              </a>
            </Button>
          </div>
        </li>
        <li>
          {t("plugins.ihk.setup.step2")}
          <pre className="mt-1 overflow-x-auto rounded-md bg-muted p-3 font-mono text-xs">
            {EXAMPLE_JSON}
          </pre>
          <p className="mt-2">{t("plugins.ihk.setup.fields")}</p>
          <dl className="mt-1 grid grid-cols-1 gap-x-3 gap-y-1 sm:grid-cols-[auto_1fr]">
            {fields.map(([name, text]) => (
              <div key={name} className="contents">
                <dt className="font-mono text-xs leading-5">{name}</dt>
                <dd className="text-muted-foreground">{text}</dd>
              </div>
            ))}
          </dl>
        </li>
        <li>{t("plugins.ihk.setup.step3")}</li>
      </ol>
      <p className="text-muted-foreground">
        {t("plugins.ihk.setup.description")}
      </p>
    </div>
  );
}
