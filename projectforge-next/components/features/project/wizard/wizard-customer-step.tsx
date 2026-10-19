"use client";

import { useTranslations } from "next-intl";
import { CustomerNumberField } from "@/components/features/customer/customer-number-field";
import { DeclaredSectionFields } from "@/components/shared/edit/declared-section-fields";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import {
  EntityEditFormProvider,
  type EntityForm,
} from "@/components/shared/form/form-context";
import { Label } from "@/components/ui/label";
import { WizardStepCard } from "@/components/features/task/wizard/wizard-step-card";
import { KUNDE_METADATA } from "@/lib/metadata/kunde.generated";
import type { SectionDef } from "@/lib/page-def/types";
import type { ValidationError } from "@/lib/rs/types";
import { WizardFieldError } from "./wizard-field-error";
import { WizardModeRadio } from "./wizard-mode-radio";

export type CustomerMode = "existing" | "new";

/**
 * The fields of a new customer: those of the customer form (CUSTOMER_PAGE) a customer is set up with,
 * the account and the description left for later.
 */
const NEW_CUSTOMER_SECTION: SectionDef<typeof KUNDE_METADATA> = {
  id: "customer",
  titleKey: "fibu.kunde._",
  fields: [
    { custom: CustomerNumberField },
    { name: "name", span: 2 },
    { name: "identifier" },
    { name: "division" },
    { name: "status" },
  ],
};

interface WizardCustomerStepProps {
  mode: CustomerMode;
  onModeChange: (mode: CustomerMode) => void;
  customer: EntityRef | null;
  onCustomerChange: (customer: EntityRef | null) => void;
  /** The form of a new customer, see ProjectWizard. */
  form: EntityForm;
  errors: ValidationError[];
}

/**
 * Step 1: the customer of the project — an existing one, or a new one created with it.
 *
 * A new customer is entered with the fields of the customer form itself (number with the hints of the
 * number ranges, name, ...), inside a form of their own so these fields find what they expect.
 */
export function WizardCustomerStep({
  mode,
  onModeChange,
  customer,
  onCustomerChange,
  form,
  errors,
}: WizardCustomerStepProps) {
  const t = useTranslations();
  return (
    <WizardStepCard
      number={1}
      heading={t("fibu.kunde._")}
      intro={t("project.wizard.customer.intro")}
    >
      <WizardModeRadio<CustomerMode>
        id="wizard-customer-mode"
        value={mode}
        onChange={onModeChange}
        ariaLabel={t("fibu.kunde._")}
        options={[
          { value: "existing", label: t("project.wizard.existing.customer") },
          { value: "new", label: t("project.wizard.new.customer") },
        ]}
      />
      {mode === "existing" ? (
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="wizard-customer" className="text-xs">
            {t("fibu.kunde._")}
          </Label>
          <EntityAutocomplete
            id="wizard-customer"
            url="customer/autosearch?search=:search"
            value={customer}
            onChange={onCustomerChange}
            className="max-w-md"
          />
          <WizardFieldError errors={errors} fieldId="customer.id" />
        </div>
      ) : (
        <EntityEditFormProvider
          value={{ form, metadata: KUNDE_METADATA, readOnly: false }}
        >
          <DeclaredSectionFields
            section={NEW_CUSTOMER_SECTION}
            metadata={KUNDE_METADATA}
          />
        </EntityEditFormProvider>
      )}
      {/* The project's verdict on its customer (a number of no range where free numbers are not allowed). */}
      <WizardFieldError errors={errors} fieldId="project.customer" />
    </WizardStepCard>
  );
}
