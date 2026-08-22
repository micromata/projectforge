"use client";

import { useMutation } from "@tanstack/react-query";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { leafKeyOf } from "@/lib/leaf-key";
import { fetchNextFreeGidNumber } from "@/lib/rs/group";
import { toast } from "@/lib/toast";
import { cn } from "@/lib/utils";
import type { GroupValues } from "./group-schema";

/**
 * The LDAP part of the group form: the posix `gidNumber` and the button that proposes a free one —
 * the fieldset `GroupPagesRest.createEditLayout` adds only where posix accounts are configured.
 *
 * Whether that is the case is the backend's decision and travels with the entity
 * (`Group.ldapPosixConfigured`): it depends on the LDAP configuration and on the user being an
 * administrator, neither of which the client can see. So this renders nothing until the loaded group
 * says otherwise, which is also why it is a custom field rather than a plain declaration.
 */
export function LdapGidField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const configured = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.ldapPosixConfigured
  );
  const gidNumber = useStore(
    form.store,
    (s: unknown) => (s as FormState).values.gidNumber
  );

  const createGid = useMutation({
    // The whole form goes with the request, as the legacy button does — see fetchNextFreeGidNumber.
    mutationFn: () => fetchNextFreeGidNumber(form.state.values),
    onSuccess: (next) => {
      if (next == null) {
        toast.error(t("validation.error.generic"));
        return;
      }
      form.setFieldValue("gidNumber" as never, next as never);
    },
    onError: (error: unknown) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });

  if (!configured) return null;

  return (
    <div className={cn("flex min-w-0 flex-wrap items-end gap-3", className)}>
      <NumberField
        name="gidNumber"
        // `ldap.gidNumber` is a label *and* the parent of its two tooltips — see leafKeyOf.
        label={t(leafKeyOf("ldap.gidNumber", t.has))}
        hint={t("ldap.gidNumber.tooltip")}
        // No metadata: GroupDO has no such property, the gid lives in the DTO alone (see types.ts).
        metadataLess
        maxDigits={7}
      />
      {/* Only for a group that has none yet, exactly as the legacy layout offers it — a gid in use
          is not replaced by a new one. */}
      {gidNumber == null && (
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={createGid.isPending}
          onClick={() => createGid.mutate()}
          title={t("ldap.gidNumber.createDefault.tooltip")}
        >
          {t("create")}
        </Button>
      )}
    </div>
  );
}

/** The slice of the form store read here; the context is deliberately untyped (form-context). */
interface FormState {
  values: Pick<GroupValues, "gidNumber" | "ldapPosixConfigured">;
}
