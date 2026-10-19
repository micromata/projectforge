"use client";

import { GuardedLink } from "@/components/shared/guarded-link";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { FolderAddIcon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { fetchProjectWizardInit } from "@/lib/rs/project-wizard";
import { PROJECT_WIZARD_ROUTE } from "./task-routes";

/** The query of who may run the project wizard, shared by its entry points and the wizard itself. */
export const PROJECT_WIZARD_INIT_QUERY_KEY = ["projectWizardInit"] as const;

/**
 * The way to the project wizard, for the users who may create customers, projects and structure
 * elements — the server's answer (`ProjectWizardRest.init`), as no single right of the client says
 * it. Its endpoints check the same, so this only hides a button that would answer 403.
 *
 * Renders nothing for everybody else, so any header may place it unconditionally: the tree, the task
 * list and the project list.
 */
export function ProjectWizardLink() {
  const t = useTranslations();
  const { data } = useQuery({
    queryKey: PROJECT_WIZARD_INIT_QUERY_KEY,
    queryFn: ({ signal }) => fetchProjectWizardInit(signal),
    staleTime: 5 * 60_000,
    retry: false,
  });
  if (!data?.allowed) return null;

  return (
    <HintTooltip text={t("project.wizard.intro")}>
      <Button asChild variant="outline" size="sm" className="gap-1.5">
        <GuardedLink href={PROJECT_WIZARD_ROUTE}>
          <HugeiconsIcon icon={FolderAddIcon} size={14} aria-hidden />
          {t("project.wizard.pageTitle")}
        </GuardedLink>
      </Button>
    </HintTooltip>
  );
}
