"use client";

import { useTranslations } from "next-intl";
import { ProjectKost2TypesField } from "@/components/features/project/project-kost2-types-field";
import { ProjectNumberField } from "@/components/features/project/project-number-field";
import { DeclaredSectionFields } from "@/components/shared/edit/declared-section-fields";
import { makeAdditionalContactsField } from "@/components/shared/form/additional-contacts-field";
import {
  EntityEditFormProvider,
  type EntityForm,
} from "@/components/shared/form/form-context";
import { WizardStepCard } from "@/components/features/task/wizard/wizard-step-card";
import { PROJEKT_METADATA } from "@/lib/metadata/projekt.generated";
import type { SectionDef } from "@/lib/page-def/types";

const ProjectAdditionalContactsField = makeAdditionalContactsField(
  "fibu.projekt.contacts.info"
);

/**
 * The fields of the new project: those of the project form (PROJECT_PAGE) without the ones the wizard
 * sets itself — customer (step 1), structure element (step 3) and project manager group (the group of
 * the managers, step 5) — and without the account, which is left for later.
 */
const PROJECT_SECTION: SectionDef<typeof PROJEKT_METADATA> = {
  id: "project",
  titleKey: "fibu.projekt._",
  fields: [
    { custom: ProjectNumberField },
    { name: "name", span: 2 },
    { name: "identifier" },
    { name: "status" },
    // The contacts as in the project form: the main one and any number of further ones.
    {
      name: "contactPerson",
      hintKey: "fibu.projekt.contacts.info",
      startsRow: true,
    },
    { custom: ProjectAdditionalContactsField, span: 2 },
    { name: "description", span: 3, rows: 3 },
    { custom: ProjectKost2TypesField, span: 3 },
  ],
};

/**
 * Step 2: the project. Its number takes the shape of the customer's number range as in the project
 * form (ProjectNumberField), for which the customer of step 1 is kept in this form's `customer`.
 */
export function WizardProjectStep({ form }: { form: EntityForm }) {
  const t = useTranslations();
  return (
    <WizardStepCard
      number={2}
      heading={t("fibu.projekt._")}
      intro={t("project.wizard.project.intro")}
    >
      <EntityEditFormProvider
        value={{ form, metadata: PROJEKT_METADATA, readOnly: false }}
      >
        <DeclaredSectionFields
          section={PROJECT_SECTION}
          metadata={PROJEKT_METADATA}
        />
      </EntityEditFormProvider>
    </WizardStepCard>
  );
}
