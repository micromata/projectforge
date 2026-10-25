"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { FormAlert } from "@/components/shared/form-alert";
import { SectionCard } from "@/components/shared/section-card";
import { formatProjectNumber } from "@/lib/number-ranges";
import { useNumberRanges } from "@/lib/rs/number-ranges";
import type { ValidationError } from "@/lib/rs/types";
import {
  WizardStructurePath,
  type WizardStructurePathProps,
} from "./wizard-structure-path";

interface WizardActionSummaryProps extends WizardStructurePathProps {
  /** The number of the new customer (only read with newCustomer). */
  customerNumber: number | null;
  /** The project number, if a project is created (otherwise the customer only). */
  projectNumber: number | null;
  /** Whether any group gets rights on the project's element. */
  anyGroup: boolean;
  /** The server's answer to the values typed so far (`ProjectWizardRest.validate`), shown at once. */
  errors: ValidationError[];
  /** The rights the groups would get (WizardPreview), below the summary. */
  preview?: ReactNode;
}

/**
 * The last step, "Action": what "Set up project" is about to do, in place of the structure wizard's note
 * on the rights (the buttons are in the pinned action bar, see ProjectWizardShell): the new customer, the new project, the path of the new structure elements and whether rights
 * are granted. A part that can't be created yet says why, from the live validation, before Finish is
 * pressed: the customer and anything else in red, the project (still being entered) as a plain note.
 *
 * The parent element and the customer's element belong to the customer's line for a new customer (its
 * element is created below the parent, or an existing one used), otherwise to the project's.
 */
export function WizardActionSummary({
  customerNumber,
  projectNumber,
  anyGroup,
  errors,
  preview,
  ...path
}: WizardActionSummaryProps) {
  const t = useTranslations();
  const info = useNumberRanges();
  const withProject = !path.customerOnly;
  const isCustomerError = (error: ValidationError) =>
    path.newCustomer &&
    (error.fieldId?.startsWith("customer.") ||
      error.fieldId === "parentTaskId" ||
      error.fieldId === "customerTaskId");
  const isProjectError = (error: ValidationError) =>
    withProject &&
    (error.fieldId?.startsWith("project.") ||
      error.fieldId === "projectTaskId" ||
      (!path.newCustomer && error.fieldId === "parentTaskId"));
  const customerErrors = messages(errors.filter(isCustomerError));
  const projectErrors = messages(errors.filter(isProjectError));
  const otherErrors = errors.filter(
    (error) => !isCustomerError(error) && !isProjectError(error)
  );
  const label = (number: string | null, name: string) =>
    [number, name].filter(Boolean).join(" ");
  return (
    <SectionCard className="flex flex-col gap-3">
      <h2 className="text-sm font-semibold">{t("task.wizard.action._")}</h2>
      <FormAlert tone="info">
        <ul className="flex list-disc flex-col gap-1 pl-4">
          {path.newCustomer &&
            (customerErrors ? (
              <li className="text-destructive">
                {t("project.wizard.action.customerNotCreated", {
                  arg0: customerErrors,
                })}
              </li>
            ) : (
              <li>
                {t("project.wizard.action.newCustomer", {
                  arg0: label(
                    customerNumber != null ? String(customerNumber) : null,
                    path.customerName
                  ),
                })}
              </li>
            ))}
          {withProject &&
            (projectErrors ? (
              <li className="text-muted-foreground">
                {t("project.wizard.action.projectNotCreated", {
                  arg0: projectErrors,
                })}
              </li>
            ) : (
              <li>
                {t("project.wizard.action.newProject", {
                  arg0: label(
                    projectNumber != null
                      ? formatProjectNumber(info, projectNumber)
                      : null,
                    path.projectName
                  ),
                })}
              </li>
            ))}
          <li className="empty:hidden">
            <WizardStructurePath {...path} />
          </li>
          {withProject && (
            <li>
              {anyGroup
                ? t("task.wizard.action.taskAndgroupsGiven")
                : t("project.wizard.action.noGroups")}
            </li>
          )}
          {otherErrors.map((error, index) => (
            <li key={index} className="text-destructive">
              {error.message}
            </li>
          ))}
        </ul>
      </FormAlert>
      {preview}
    </SectionCard>
  );
}

/** The messages of the errors, each once, as one text; empty if there are none. */
function messages(errors: ValidationError[]): string {
  return [...new Set(errors.map((error) => error.message))].join(" ");
}
