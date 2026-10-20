"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Tick02Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { GuardedLink } from "@/components/shared/guarded-link";
import { SectionCard } from "@/components/shared/section-card";
import {
  HIGHLIGHT_ID_PARAM,
  TASK_TREE_ROUTE,
} from "@/components/shared/tasks/task-routes";
import { ROLE_KEYS } from "@/components/features/task/wizard/result-model";
import { WizardResult } from "@/components/features/task/wizard/wizard-result";
import type { ProjectWizardResult } from "@/lib/rs/project-wizard";

/**
 * What the project wizard set up: customer, project, structure element and groups, each a link to its
 * own page and marked whether it is new, and below it the access rights as the structure wizard reports
 * them (WizardResult). A customer created alone has neither project nor rights, and maybe no element.
 */
export function WizardProjectResult({
  result,
  onAgain,
}: {
  result: ProjectWizardResult;
  onAgain: () => void;
}) {
  const t = useTranslations();
  const created = t("project.wizard.result.created");
  const existing = t("project.wizard.result.existing");
  const elementId = result.taskId ?? result.customerTaskId;
  const treeHref =
    elementId != null
      ? `${TASK_TREE_ROUTE}?${HIGHLIGHT_ID_PARAM}=${elementId}`
      : TASK_TREE_ROUTE;
  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-sm font-semibold">
        {t("project.wizard.result.title")}
      </h2>
      <SectionCard className="px-4 py-3">
        <ul className="flex flex-col gap-2 text-sm">
          <Row
            label={t("fibu.kunde._")}
            name={result.customerName}
            href={`/customer/${result.customerId}`}
            status={result.customerCreated ? created : existing}
            isNew={result.customerCreated}
          />
          {result.customerTaskId != null && (
            <Row
              label={t("project.wizard.result.customerElement")}
              name={result.customerName}
              href={`${TASK_TREE_ROUTE}?${HIGHLIGHT_ID_PARAM}=${result.customerTaskId}`}
              status={result.customerCreated ? created : existing}
              isNew={result.customerCreated}
            />
          )}
          {result.projectId != null && (
            <>
              <Row
                label={t("fibu.projekt._")}
                name={result.projectName}
                href={`/project/${result.projectId}`}
                status={created}
                isNew
              />
              <Row
                label={t("project.wizard.result.projectElement")}
                name={result.projectName}
                href={treeHref}
                status={created}
                isNew
              />
            </>
          )}
          {result.groups.map((group) => (
            <Row
              key={group.groupType}
              label={t(ROLE_KEYS[group.groupType])}
              name={group.name}
              href={`/group/${group.id}`}
              status={group.created ? created : existing}
              isNew={group.created}
            />
          ))}
        </ul>
      </SectionCard>
      {result.access ? (
        <WizardResult
          result={result.access}
          onAgain={onAgain}
          treeHref={treeHref}
        />
      ) : (
        // A customer alone: no rights to report, only the way on (as WizardResult offers it).
        <div className="flex items-center gap-2">
          <Button type="button" variant="outline" onClick={onAgain}>
            {t("task.wizard.result.again")}
          </Button>
          <Button asChild>
            <GuardedLink href={treeHref}>
              {t("task.wizard.result.toTree")}
            </GuardedLink>
          </Button>
        </div>
      )}
    </div>
  );
}

function Row({
  label,
  name,
  href,
  status,
  isNew,
}: {
  label: string;
  name?: string | null;
  href: string;
  status: string;
  isNew: boolean;
}) {
  return (
    <li className="flex items-center gap-2">
      <HugeiconsIcon
        icon={Tick02Icon}
        className="size-4 shrink-0 text-emerald-600"
        aria-hidden
      />
      <span className="grow">
        {label}{" "}
        <GuardedLink
          href={href}
          className="font-medium text-primary hover:underline"
        >
          {name ?? ""}
        </GuardedLink>
      </span>
      <Badge variant={isNew ? "default" : "outline"}>{status}</Badge>
    </li>
  );
}
