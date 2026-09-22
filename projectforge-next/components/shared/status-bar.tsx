"use client";

import { useTranslations } from "next-intl";
import { useAuth } from "@/hooks/use-auth";

const WEBSITE_URL = "https://www.projectforge.org";
const COMPANY_URL = "https://www.micromata.com";
const NEWS_URL = "https://www.projectforge.org/changelog-posts/";

/**
 * The status strip pinned to the bottom of every authenticated page, mirroring Wicket's footer:
 * copyright and company on the left, version and build date on the right.
 *
 * Version and build date come from `userStatus.systemData` (via {@link useAuth}), not from the
 * public `/rsPublic/systemStatus` — the latter masks both "for security reasons" (see
 * SystemStatusRest.publicSystemData) and would render placeholders here.
 */
export function StatusBar() {
  const t = useTranslations("statusBar");
  const { systemData } = useAuth();

  return (
    <footer className="flex h-5 shrink-0 items-center justify-between gap-4 bg-status-bar px-4 text-[0.6875rem] leading-none text-status-bar-foreground">
      <div className="flex min-w-0 items-center gap-1.5 truncate">
        <a
          href={WEBSITE_URL}
          target="_blank"
          rel="noreferrer"
          className="truncate hover:underline"
        >
          {t("copyright", { years: systemData?.copyRightYears ?? "" })}
        </a>
        <span aria-hidden>|</span>
        <a
          href={COMPANY_URL}
          target="_blank"
          rel="noreferrer"
          className="hover:underline"
        >
          {t("company")}
        </a>
      </div>
      {systemData?.version ? (
        <a
          href={NEWS_URL}
          target="_blank"
          rel="noreferrer"
          className="shrink-0 truncate hover:underline"
        >
          {t("version", {
            version: systemData.version,
            buildDate: systemData.buildDate,
          })}
        </a>
      ) : null}
    </footer>
  );
}
