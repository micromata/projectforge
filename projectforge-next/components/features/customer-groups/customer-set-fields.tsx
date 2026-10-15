"use client";

import { useTranslations } from "next-intl";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import { InputField } from "@/components/shared/form/input-field";
import { TextMemberField } from "./text-member-field";

/** The fields a customer group and a business unit share: the name and the members. */
export function CustomerSetFields({ prefix }: { prefix: string }) {
  const t = useTranslations();
  return (
    <>
      <InputField
        name={`${prefix}name`}
        label={t("fibu.customerGroups.name")}
      />
      <EntityMultiAutocompleteField
        name={`${prefix}customers`}
        label={t("fibu.customerGroups.customers")}
        entity="customer"
        sorted
        className="md:col-span-2"
      />
      <TextMemberField
        name={`${prefix}texts`}
        label={t("fibu.customerGroups.texts")}
        hint={t("fibu.customerGroups.textsHint")}
        className="md:col-span-2"
      />
    </>
  );
}
