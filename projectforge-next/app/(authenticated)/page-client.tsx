"use client";

import { useTranslations } from "next-intl";
import { MicromataIcon } from "@/components/shared/micromata-icon";
import { PageShell } from "@/components/shared/page-shell";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const WEBSITE_URL = "https://www.projectforge.org";
const SOURCES_URL = "https://github.com/micromata/projectforge";

export function HomePageClient() {
  const t = useTranslations("index");

  return (
    <PageShell>
      <div className="p-6">
        <Card className="max-w-xl">
          <CardHeader className="flex flex-row items-center gap-3">
            <MicromataIcon size={32} />
            <CardTitle className="text-base">{t("welcome")}</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-2">
            <ExternalLink label={t("website")} href={WEBSITE_URL} />
            <ExternalLink label={t("development")} href={SOURCES_URL} />
          </CardContent>
        </Card>
      </div>
    </PageShell>
  );
}

/** A labelled link off this app, shown with the bare url so it is recognisable as external. */
function ExternalLink({ label, href }: { label: string; href: string }) {
  return (
    <p>
      {label}:{" "}
      <a
        href={href}
        target="_blank"
        rel="noreferrer"
        className="text-primary underline underline-offset-2"
      >
        {href.replace(/^https:\/\//, "")}
      </a>
    </p>
  );
}
