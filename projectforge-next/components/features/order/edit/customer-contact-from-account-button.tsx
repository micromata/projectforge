"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { HugeiconsIcon } from "@hugeicons/react";
import { UserAccountIcon } from "@hugeicons/core-free-icons";
import { toast } from "@/lib/toast";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { fetchCustomerContactFromAccount } from "@/lib/rs/order";

const CONTACT_FIELDS = [
  "customerContactPerson",
  "customerContactDepartment",
  "customerContactEmail",
] as const;

/**
 * "Add from account": the empty customer contact fields (BT-56, BT-58) of this order from the account of its
 * customer. The contact of the order takes precedence over the account's, so only what is empty is filled, and
 * checked twice: the backend fills nothing that was posted, and a field typed into while the request was out is
 * left alone as well.
 *
 * Disabled while the order names no customer from the list: a free text customer has no account.
 */
export function CustomerContactFromAccountButton() {
  const t = useTranslations();
  const form = useEntityEditForm();
  const hasCustomer = useStore(
    form.store,
    (s: unknown) =>
      (s as { values: { customer?: { id?: number } | null } }).values.customer
        ?.id != null
  );

  const run = useMutation({
    mutationFn: async () => {
      const filled = await fetchCustomerContactFromAccount(form.state.values);
      let count = 0;
      for (const name of CONTACT_FIELDS) {
        const value = filled[name];
        if (value && !form.getFieldValue(name)) {
          form.setFieldValue(name, value);
          count++;
        }
      }
      return count;
    },
    onSuccess: (count) =>
      count > 0
        ? toast.success(
            t("fibu.auftrag.customerContact.fromAccountDone", { arg0: count })
          )
        : toast.info(t("fibu.auftrag.customerContact.fromAccountNothing")),
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  return (
    <HintTooltip text={t("fibu.auftrag.customerContact.fromAccountHint")}>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        disabled={!hasCustomer || run.isPending}
        onClick={() => run.mutate()}
      >
        {run.isPending ? (
          <Spinner className="h-3.5 w-3.5 border-2" />
        ) : (
          <HugeiconsIcon icon={UserAccountIcon} size={14} aria-hidden />
        )}
        {t("fibu.auftrag.customerContact.fromAccount")}
      </Button>
    </HintTooltip>
  );
}
