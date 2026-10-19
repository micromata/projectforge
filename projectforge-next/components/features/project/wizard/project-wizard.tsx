"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useForm, useStore } from "@tanstack/react-form";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CUSTOMER_LIST_QUERY_KEY } from "@/components/features/customer/customer.page";
import { emptyCustomerValues } from "@/components/features/customer/customer-values";
import {
  CUSTOMER_FIELDS,
  type CustomerValues,
} from "@/components/features/customer/customer-schema";
import {
  PROJECT_FIELDS,
  type ProjectValues,
} from "@/components/features/project/project-schema";
import {
  emptyProjectValues,
  toFormValues as toProjectFormValues,
} from "@/components/features/project/project-values";
import { PROJECT_LIST_QUERY_KEY } from "@/components/features/project/project.page";
import type { ProjectDetail } from "@/components/features/project/types";
import { GROUP_STEPS } from "@/components/features/task/wizard/types";
import { WizardActionStep } from "@/components/features/task/wizard/wizard-action-step";
import { suggestGroupName } from "@/components/features/task/wizard/wizard-group-step";
import { WizardPreview } from "@/components/features/task/wizard/wizard-preview";
import type { EntityRef } from "@/components/shared/entity-autocomplete";
import { FormAlert } from "@/components/shared/form-alert";
import { Spinner } from "@/components/shared/spinner";
import { PROJECT_WIZARD_INIT_QUERY_KEY } from "@/components/shared/tasks/project-wizard-link";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { fetchNew } from "@/lib/rs/client";
import {
  executeProjectWizard,
  fetchCustomerTask,
  fetchProjectWizardInit,
  validateProjectWizard,
  type ProjectWizardGroup,
  type ProjectWizardRequest,
  type ProjectWizardResult,
} from "@/lib/rs/project-wizard";
import type { ValidationError } from "@/lib/rs/types";
import { applyServerValidationErrors } from "@/lib/validation/server-errors";
import { navigateInGesture } from "@/lib/navigate-in-gesture";
import {
  initialRoles,
  ROLE_FIELDS,
  type RoleState,
  type RoleStates,
} from "./types";
import { WizardCustomerStep, type CustomerMode } from "./wizard-customer-step";
import { WizardParentTaskStep } from "./wizard-parent-task-step";
import { WizardProjectStep } from "./wizard-project-step";
import { WizardProjectResult } from "./wizard-project-result";
import { WizardRoleStep } from "./wizard-role-step";

/**
 * The project wizard: sets up a project in one go — its customer (an existing or a new one), the
 * project, a structure element for it below a picked one, the groups of its team, managers and
 * externals (existing ones or new ones with their members), and their rights on the new element, as
 * the structure wizard grants them (TaskWizard, whose preview and report it shows as well).
 *
 * The customer and the project are entered with the fields of their own forms, inside a TanStack form
 * each, as those fields expect one; the rest is plain state, as in the structure wizard. Nothing is
 * written before Finish, and then everything at once (`ProjectWizardRest.execute`, one transaction).
 *
 * The backend checks the values while they are typed (`validate`), for the preview of the rights. Its
 * complaints are shown only once Finish was pressed, not while a value is still on its way.
 */
export function ProjectWizard() {
  const t = useTranslations();
  const router = useRouter();
  const queryClient = useQueryClient();

  const init = useQuery({
    queryKey: PROJECT_WIZARD_INIT_QUERY_KEY,
    queryFn: ({ signal }) => fetchProjectWizardInit(signal),
    staleTime: 5 * 60_000,
    retry: false,
  });
  const allowed = init.data?.allowed === true;
  // The preset of a new project, which carries the cost 2 types it may get.
  const projectPreset = useQuery({
    queryKey: ["projectWizardPreset"],
    queryFn: ({ signal }) => fetchNew<ProjectDetail>("project", {}, signal),
    enabled: allowed,
    staleTime: Infinity,
  });

  const [customerMode, setCustomerMode] = useState<CustomerMode>("existing");
  const [customer, setCustomer] = useState<EntityRef | null>(null);
  const customerForm = useForm({ defaultValues: emptyCustomerValues() });
  const projectForm = useForm({ defaultValues: emptyProjectValues() });
  const [parentTaskId, setParentTaskId] = useState<number | null>(null);
  const [roles, setRoles] = useState<RoleStates>(initialRoles);
  const [attempted, setAttempted] = useState(false);
  const [executeErrors, setExecuteErrors] = useState<string[]>([]);
  const [result, setResult] = useState<ProjectWizardResult | null>(null);

  const customerValues = useStore(
    customerForm.store,
    (s) => s.values
  ) as CustomerValues;
  const projectValues = useStore(
    projectForm.store,
    (s) => s.values
  ) as ProjectValues;

  // The cost 2 types of the preset, once it is there; a reset keeps them (see onAgain).
  useEffect(() => {
    if (!projectPreset.data) return;
    projectForm.setFieldValue(
      "kost2Arts",
      toProjectFormValues(projectPreset.data).kost2Arts
    );
  }, [projectPreset.data, projectForm]);

  // The customer of step 1 in the project form, whose number field takes its range from it. The id of
  // a customer is its number, so a new one is there as soon as its number is typed.
  const customerNummer =
    customerMode === "existing"
      ? (customer?.id ?? null)
      : (customerValues.nummer ?? null);
  useEffect(() => {
    const current = projectForm.getFieldValue("customer") as {
      id: number;
    } | null;
    if ((current?.id ?? null) === customerNummer) return;
    projectForm.setFieldValue(
      "customer",
      customerNummer != null ? { id: customerNummer } : null
    );
  }, [customerNummer, projectForm]);

  // The element of an existing customer, as its projects tell, preset in step 3 when the customer is
  // picked. A new customer's element doesn't exist yet: there the parent of it is picked.
  const pickCustomer = (picked: EntityRef | null) => {
    setCustomer(picked);
    if (!picked) return;
    void queryClient
      .fetchQuery({
        queryKey: ["projectWizardCustomerTask", picked.id],
        queryFn: ({ signal }) => fetchCustomerTask(picked.id, signal),
        staleTime: 60_000,
      })
      .then((taskId) => {
        if (taskId != null) setParentTaskId(taskId);
      })
      .catch(() => undefined);
  };

  const projectName = projectValues.name?.trim() ?? "";
  const suggestedNames = useMemo(
    () =>
      Object.fromEntries(
        GROUP_STEPS.map((step) => [
          step.key,
          suggestGroupName(step, projectName, t),
        ])
      ),
    [projectName, t]
  );

  const request: ProjectWizardRequest = {
    customer:
      customerMode === "existing"
        ? { existing: true, id: customer?.id ?? null }
        : {
            nummer: customerValues.nummer,
            name: customerValues.name,
            identifier: customerValues.identifier,
            division: customerValues.division,
            status: customerValues.status,
          },
    project: {
      nummer: projectValues.nummer as number,
      projektnummer: projectValues.projektnummer,
      name: projectValues.name,
      identifier: projectValues.identifier,
      status: projectValues.status,
      description: projectValues.description,
      projectManagerId: projectValues.projectManager?.id ?? null,
      headOfBusinessManagerId: projectValues.headOfBusinessManager?.id ?? null,
      salesManagerId: projectValues.salesManager?.id ?? null,
      kost2ArtIds: projectValues.kost2Arts
        .filter((art) => art.selected)
        .map((art) => art.id),
    },
    parentTaskId,
    ...Object.fromEntries(
      GROUP_STEPS.map((step) => [
        ROLE_FIELDS[step.key],
        toGroup(roles[step.key], suggestedNames[step.key]),
      ])
    ),
  };

  // Asked again as the user types, but not on every key stroke.
  const debouncedRequest = useDebouncedValue(request, 400);
  const validation = useQuery({
    queryKey: ["projectWizardValidate", debouncedRequest],
    queryFn: ({ signal }) => validateProjectWizard(debouncedRequest, signal),
    enabled: allowed,
    placeholderData: (previous) => previous,
    staleTime: Infinity,
  });
  const errors: ValidationError[] = attempted
    ? (validation.data?.validationErrors ?? [])
    : [];

  // The errors of the values inside the two forms go onto their fields; they are cleared there as soon as
  // the field is changed, and come back with the next answer if the value is still wrong.
  useEffect(() => {
    applyServerValidationErrors(
      customerForm,
      prefixed(errors, "customer."),
      CUSTOMER_FIELDS
    );
    applyServerValidationErrors(
      projectForm,
      prefixed(errors, "project."),
      PROJECT_FIELDS,
      ["kost2Arts"]
    );
    // `errors` is rebuilt each render; its content changes with the answer only.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [attempted, validation.data, customerForm, projectForm]);

  const execute = useMutation({
    mutationFn: () => executeProjectWizard(request),
    onSuccess: (outcome) => {
      if (outcome.kind === "ok") {
        setResult(outcome.result);
        setExecuteErrors([]);
        // The new customer and project are offered by the lists and selections from now on.
        void queryClient.invalidateQueries({
          queryKey: CUSTOMER_LIST_QUERY_KEY,
        });
        void queryClient.invalidateQueries({
          queryKey: PROJECT_LIST_QUERY_KEY,
        });
        return;
      }
      setAttempted(true);
      void validation.refetch();
      // The field errors show beside their fields (see above); only the others need saying here.
      setExecuteErrors(
        outcome.validationErrors
          .filter((error) => !error.fieldId)
          .map((error) => error.message)
      );
    },
    onError: (err: unknown) =>
      setExecuteErrors([err instanceof Error ? err.message : String(err)]),
  });

  const reset = () => {
    setResult(null);
    setAttempted(false);
    setExecuteErrors([]);
    setCustomerMode("existing");
    setCustomer(null);
    customerForm.reset(emptyCustomerValues());
    projectForm.reset({
      ...emptyProjectValues(),
      kost2Arts: projectPreset.data
        ? toProjectFormValues(projectPreset.data).kost2Arts
        : [],
    });
    setParentTaskId(null);
    setRoles(initialRoles());
  };

  if (init.isPending) {
    return <Spinner />;
  }
  if (!allowed) {
    return (
      <div className="flex max-w-3xl flex-col">
        <FormAlert tone="error">{t("project.wizard.notAllowed")}</FormAlert>
      </div>
    );
  }
  if (result) {
    return (
      <div className="flex max-w-3xl flex-col gap-4">
        <WizardProjectResult result={result} onAgain={reset} />
      </div>
    );
  }

  const preview = validation.data?.preview;
  const anyGroup = GROUP_STEPS.some(
    (step) => toGroup(roles[step.key], suggestedNames[step.key]) != null
  );
  return (
    <div className="flex flex-col gap-4">
      <div className="flex max-w-3xl flex-col gap-4">
        {executeErrors.map((message) => (
          <FormAlert key={message} tone="error">
            {message}
          </FormAlert>
        ))}
        <p className="text-sm text-muted-foreground">
          {t("project.wizard.intro")}
        </p>
        <WizardCustomerStep
          mode={customerMode}
          onModeChange={setCustomerMode}
          customer={customer}
          onCustomerChange={pickCustomer}
          form={customerForm}
          errors={errors}
        />
        <WizardProjectStep form={projectForm} />
        <WizardParentTaskStep
          newCustomer={customerMode === "new"}
          customerName={customerValues.name?.trim() ?? ""}
          rootTaskId={init.data?.rootTaskId}
          parentTaskId={parentTaskId}
          onChange={setParentTaskId}
          projectName={projectName}
          errors={errors}
        />
        {GROUP_STEPS.map((step, index) => (
          <WizardRoleStep
            key={step.key}
            step={step}
            number={index + 4}
            value={roles[step.key]}
            onChange={(value: RoleState) =>
              setRoles((previous) => ({ ...previous, [step.key]: value }))
            }
            suggestedName={suggestedNames[step.key]}
            errors={errors}
          />
        ))}
      </div>
      {preview && anyGroup && (
        <div className="flex max-w-5xl flex-col">
          <WizardPreview preview={preview} isFetching={validation.isFetching} />
        </div>
      )}
      <div className="flex max-w-3xl flex-col">
        <WizardActionStep
          hasAction={anyGroup}
          canFinish={parentTaskId != null}
          isPending={execute.isPending}
          onCancel={() => navigateInGesture(router, "/project")}
          onFinish={() => execute.mutate()}
          finishLabel={t("project.wizard.finish")}
        />
      </div>
    </div>
  );
}

/** What a role step sends: an existing group by its id, a new one by name and members, or nothing. */
function toGroup(
  role: RoleState,
  suggestedName: string | null | undefined
): ProjectWizardGroup | null {
  if (role.mode === "existing") {
    return role.group ? { id: role.group.id } : null;
  }
  if (role.mode === "new") {
    const name = (role.name ?? suggestedName ?? "").trim();
    return name
      ? { name, userIds: role.members.map((member) => member.id) }
      : null;
  }
  return null;
}

/** The errors of one form, with the field ids it knows them by. */
function prefixed(
  errors: ValidationError[],
  prefix: string
): ValidationError[] {
  return errors
    .filter((error) => error.fieldId?.startsWith(prefix))
    .map((error) => ({
      ...error,
      fieldId: error.fieldId.slice(prefix.length),
    }));
}
