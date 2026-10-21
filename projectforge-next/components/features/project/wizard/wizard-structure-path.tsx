"use client";

import { Fragment } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchTaskInfo } from "@/lib/rs/task";

export interface WizardStructurePathProps {
  parentTaskId: number | null;
  /** Whether the customer is created with the project, which gets an element of its own. */
  newCustomer: boolean;
  /** A new customer without project: no element for a project. */
  customerOnly: boolean;
  /** The title of the customer's new element, the customer's name. */
  customerName: string;
  /** The title of the project's new element, the project's name. */
  projectName: string;
  /** The root of the tree, which the path leaves out as the tree's breadcrumbs do. */
  rootTaskId?: number | null;
}

/**
 * The path of the structure elements the wizard creates below the picked one, e.g.
 * "Customers → ACME (new) → Website (new)"; nothing while no element is picked or nothing new is named.
 * Shown in step 3 and in the summary of the last card.
 */
export function WizardStructurePath({
  parentTaskId,
  newCustomer,
  customerOnly,
  customerName,
  projectName,
  rootTaskId,
}: WizardStructurePathProps) {
  const t = useTranslations();
  // The same query as the select control's, so the picked element's path costs no second request.
  const { data: picked } = useQuery({
    queryKey: ["taskInfo", parentTaskId],
    queryFn: ({ signal }) => fetchTaskInfo(parentTaskId!, signal),
    enabled: parentTaskId != null,
    staleTime: Infinity,
  });
  const path: { title: string; isNew: boolean }[] = [
    ...(picked
      ? [...(picked.path ?? []), picked]
          .filter((node) => !node.root && node.id !== rootTaskId)
          .map((node) => ({ title: node.title ?? "", isNew: false }))
      : []),
    ...(newCustomer && customerName
      ? [{ title: customerName, isNew: true }]
      : []),
    ...(projectName && !customerOnly
      ? [{ title: projectName, isNew: true }]
      : []),
  ];
  if (parentTaskId == null || !path.some((segment) => segment.isNew)) {
    return null;
  }
  return (
    <p className="flex flex-wrap items-center gap-x-1.5 text-xs text-muted-foreground">
      <span>{t("project.wizard.parentTask.newPath")}</span>
      {path.map((segment, index) => (
        <Fragment key={index}>
          {index > 0 && <span aria-hidden>→</span>}
          <span
            className={
              segment.isNew ? "font-medium text-foreground" : undefined
            }
          >
            {segment.title}
            {segment.isNew && ` (${t("project.wizard.preview.newElement")})`}
          </span>
        </Fragment>
      ))}
    </p>
  );
}
