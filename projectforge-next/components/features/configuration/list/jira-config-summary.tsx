"use client";

import { useTranslations } from "next-intl";
import { jiraProjectKeys, parseJiraConfig } from "../jira-config";

/** The JIRA settings in one line (default url, number of servers and their projects), for the list. */
export function JiraConfigSummary({ json }: { json: string | null | undefined }) {
  const t = useTranslations();
  const { config, invalid } = parseJiraConfig(json);
  if (invalid) {
    return <span className="text-warning">{t("administration.configuration.jira.invalidJson")}</span>;
  }
  const parts: string[] = [];
  if (config.browseBaseUrl.trim()) {
    parts.push(config.browseBaseUrl.trim());
  }
  if (config.servers.length > 0) {
    const projects = config.servers.flatMap((server) => jiraProjectKeys(server.projects));
    parts.push(
      `${t("administration.configuration.jira.summary.servers", { arg0: config.servers.length })}` +
        (projects.length > 0 ? `: ${projects.join(", ")}` : "")
    );
  }
  return <span className="line-clamp-2 break-all">{parts.join(" · ")}</span>;
}
