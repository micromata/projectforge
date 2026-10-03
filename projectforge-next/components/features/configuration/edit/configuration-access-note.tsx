"use client";

import { useTranslations } from "next-intl";
import Link from "next/link";
import { FormAlert } from "@/components/shared/form-alert";
import { useEntityData } from "@/components/shared/form/form-context";
import type { ConfigurationDetail } from "../types";

/**
 * Why a parameter is shown but can't be saved here: either it is maintained on a page of its own
 * (`editPage`, the customer groups), linked from here, or it belongs to the other side
 * (`ConfigurationParam.getEditors` — admins see the finance parameters read-only). Nothing for a
 * parameter the user may change.
 */
export function ConfigurationAccessNote() {
  const t = useTranslations();
  const data = useEntityData<ConfigurationDetail>();
  if (data?.editPage) {
    return (
      <FormAlert tone="info">
        {t("administration.configuration.editPage.hint")}{" "}
        {/* next/link prepends the app's basePath (/next) itself — see menu-url.ts. */}
        <Link
          href={`/${data.editPage}?returnTo=/configuration`}
          className="font-medium text-primary underline underline-offset-2"
        >
          {t("administration.configuration.editPage.link")}
        </Link>
      </FormAlert>
    );
  }
  if (data?.writeAccess === false) {
    return (
      <FormAlert tone="info">
        {t("administration.configuration.readOnly")}
      </FormAlert>
    );
  }
  return null;
}
