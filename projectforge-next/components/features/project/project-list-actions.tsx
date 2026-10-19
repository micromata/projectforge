"use client";

import { ProjectWizardLink } from "@/components/shared/tasks/project-wizard-link";

/**
 * The actions of the project list: the project wizard, which sets up a new project together with its
 * customer, structure element, groups and access rights. Hidden for users who may not run it.
 */
export function ProjectListActions() {
  return <ProjectWizardLink />;
}
