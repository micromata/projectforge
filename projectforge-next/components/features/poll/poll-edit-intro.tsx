"use client";

import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import { useEntityData } from "@/components/shared/form/form-context";
import type { PollDetail } from "./types";

/** Above the form: what happens with a poll (mails, deadline), or that it is finished and stays as it is. */
export function PollEditIntro() {
  const t = useTranslations();
  const poll = useEntityData<PollDetail>();
  if (poll?.state === "FINISHED") {
    return <FormAlert tone="info">{t("poll.answer.notOpen")}</FormAlert>;
  }
  if (poll?.template) {
    return <FormAlert tone="info">{t("poll.template.info")}</FormAlert>;
  }
  return <FormAlert tone="info">{t("poll.info")}</FormAlert>;
}
