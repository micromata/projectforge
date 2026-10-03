"use client";

import { useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import { EntityEditActions } from "@/components/shared/edit/entity-edit-actions";
import { EntityEditFormProvider } from "@/components/shared/form/form-context";
import { useEntityEditForm } from "@/hooks/use-entity-edit-form";
import { useFormatContext } from "@/hooks/use-format";
import { useSubmitShortcut } from "@/hooks/use-submit-shortcut";
import {
  confirmLeaveUnsavedChanges,
  useUnsavedChangesWarning,
} from "@/hooks/use-unsaved-changes-warning";
import { saveCustomerGroups } from "@/lib/rs/customer-groups";
import { BusinessUnitList } from "./business-unit-list";
import { GroupList } from "./group-list";
import { UnassignedCustomers } from "./unassigned-customers";
import { useLiveValidation } from "./use-live-validation";
import {
  CUSTOMER_GROUPS_FIELDS,
  customerGroupsSchema,
  FORM_METADATA,
} from "./schema";
import type { CustomerGroupsData, CustomerGroupsValues } from "./types";
import { EMPTY_VALUES, toFormValues, toPayload } from "./values";

/**
 * The pages leading here that cancelling returns to, named by the caller as `?returnTo=`: the
 * configuration list (the parameter's row) and the customers. Only these are followed, so the url
 * needs no sanitizing.
 */
const RETURN_ROUTES = ["/configuration", "/customer"];

const save = (values: CustomerGroupsValues) =>
  saveCustomerGroups(toPayload(values));

/**
 * The editor of the customer groups and business units: both lists in one form, saved as a whole —
 * they are one configuration value, and the rules spanning both (a group in at most one business unit)
 * can only be checked together.
 */
export function CustomerGroupsForm({ data }: { data: CustomerGroupsData }) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const router = useRouter();
  const requested = useSearchParams().get("returnTo");
  const returnRoute = RETURN_ROUTES.find((route) => route === requested);
  const format = useFormatContext();
  const toSortedFormValues = useCallback(
    (loaded: CustomerGroupsData) => toFormValues(loaded, format),
    [format]
  );
  const { form, isDirty, isSubmitting } = useEntityEditForm<
    CustomerGroupsValues,
    CustomerGroupsData
  >({
    data,
    toFormValues: toSortedFormValues,
    defaultValues: EMPTY_VALUES,
    schema: customerGroupsSchema,
    fieldNames: CUSTOMER_GROUPS_FIELDS,
    arrayFieldNames: CUSTOMER_GROUPS_FIELDS,
    listRoute: "/customerGroups",
    savedMessage: t("fibu.customerGroups.saved"),
    // Stays on the page; the refetch brings the new `lastUpdate` the next save is checked against.
    // The checklists offer the groups, so their values are stale as well.
    onSaved: () => {
      void queryClient.invalidateQueries({ queryKey: ["customerGroups"] });
      void queryClient.invalidateQueries({ queryKey: ["filterListValues"] });
    },
    save,
  });
  useUnsavedChangesWarning(isDirty);
  useLiveValidation(form, isDirty);
  const onKeyDown = useSubmitShortcut(
    () => void form.handleSubmit(),
    isDirty && !isSubmitting
  );

  return (
    <EntityEditFormProvider
      value={{ form, metadata: FORM_METADATA, readOnly: false, data }}
    >
      <form
        className="flex min-h-0 flex-1 flex-col overflow-hidden"
        onKeyDown={onKeyDown}
        onSubmit={(event) => {
          event.preventDefault();
          void form.handleSubmit();
        }}
      >
        <div className="flex-1 overflow-y-auto px-4 pb-8 pt-2">
          <div className="mx-auto flex w-full max-w-4xl flex-col gap-6">
            <p className="text-sm text-muted-foreground">
              {t("fibu.customerGroups.intro")}
            </p>
            <section className="flex flex-col gap-2">
              <h2 className="text-sm font-semibold">
                {t("fibu.customerGroups.groups")}
              </h2>
              <GroupList />
            </section>
            <section className="flex flex-col gap-2">
              <h2 className="text-sm font-semibold">
                {t("fibu.businessUnits.title")}
              </h2>
              <BusinessUnitList />
            </section>
            <UnassignedCustomers />
          </div>
        </div>
        <EntityEditActions
          // Back to the caller, asking first if there are changes. Opened from the menu there is
          // nothing to leave for: cancelling takes back the changes since loading or the last save.
          onCancel={() => {
            if (!returnRoute) {
              form.reset();
              return;
            }
            void confirmLeaveUnsavedChanges().then((leave) => {
              if (leave) router.push(returnRoute);
            });
          }}
          canSave
          isSaving={isSubmitting}
          isDirty={isDirty}
          allowSaveUnchanged={false}
          lastSaved={null}
        />
      </form>
    </EntityEditFormProvider>
  );
}
