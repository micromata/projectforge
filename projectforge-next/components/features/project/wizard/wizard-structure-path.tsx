"use client";

import { Fragment } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchTaskInfo } from "@/lib/rs/task";
import { cn } from "@/lib/utils";

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
  /** The existing element that becomes the customer's (new customer only), instead of a new one. */
  reusedCustomerTaskId?: number | null;
  /** The existing element that becomes the project's, instead of a new one. */
  reusedProjectTaskId?: number | null;
  /** The root of the tree, which the path leaves out as the tree's breadcrumbs do. */
  rootTaskId?: number | null;
}

/**
 * The path of the structure elements the wizard creates or uses, e.g. "Customers → ACME (new) →
 * Website (new)", or "Customers → ACME (existing) → Website (new)" with an existing element; nothing while
 * no element is picked or nothing is named. Shown in step 3 and in the summary of the last card.
 *
 * The path is the one of the deepest existing element: the project's, the customer's, or the picked one.
 */
export function WizardStructurePath({
  parentTaskId,
  newCustomer,
  customerOnly,
  customerName,
  projectName,
  reusedCustomerTaskId,
  reusedProjectTaskId,
  rootTaskId,
}: WizardStructurePathProps) {
  const t = useTranslations();
  const projectReused = !customerOnly && reusedProjectTaskId != null;
  const deepestId = projectReused
    ? reusedProjectTaskId
    : (reusedCustomerTaskId ?? parentTaskId);
  const reused = new Set([reusedCustomerTaskId, reusedProjectTaskId]);
  // The same query as the select control's, so the picked element's path costs no second request.
  const { data: picked } = useQuery({
    queryKey: ["taskInfo", deepestId],
    queryFn: ({ signal }) => fetchTaskInfo(deepestId!, signal),
    enabled: deepestId != null,
    staleTime: Infinity,
  });
  const path: { title: string; status?: "new" | "existing" }[] = [
    ...(picked
      ? [...(picked.path ?? []), picked]
          .filter((node) => !node.root && node.id !== rootTaskId)
          .map((node) => ({
            title: node.title ?? "",
            status: reused.has(node.id) ? ("existing" as const) : undefined,
          }))
      : []),
    ...(newCustomer && reusedCustomerTaskId == null && customerName
      ? [{ title: customerName, status: "new" as const }]
      : []),
    ...(projectName && !customerOnly && !projectReused
      ? [{ title: projectName, status: "new" as const }]
      : []),
  ];
  if (deepestId == null || !path.some((segment) => segment.status)) {
    return null;
  }
  return (
    <p className="flex flex-wrap items-center gap-x-1.5 text-xs text-muted-foreground">
      <span>{t("project.wizard.parentTask.newPath")}</span>
      {path.map((segment, index) => (
        <Fragment key={index}>
          {index > 0 && <span aria-hidden>→</span>}
          <span className={cn(segment.status && "font-medium text-foreground")}>
            {segment.title}
            {segment.status === "new" &&
              ` (${t("project.wizard.preview.newElement")})`}
            {segment.status === "existing" &&
              ` (${t("project.wizard.preview.existingElement")})`}
          </span>
        </Fragment>
      ))}
    </p>
  );
}
