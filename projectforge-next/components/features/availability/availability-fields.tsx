"use client";

import { useTranslations } from "next-intl";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { EntityMultiAutocompleteField } from "@/components/shared/form/entity-multi-autocomplete-field";
import {
  useEntityData,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { SelectField } from "@/components/shared/form/select-field";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { AVAILABILITY_METADATA } from "@/lib/metadata/availability.generated";
import { leafKeyOf } from "@/lib/leaf-key";
import type { AvailabilityDetail } from "./types";

/**
 * Whose entry it is: a user, also a freelancer without an employee record. Only HR staff may choose
 * (`Availability.userEditable`); everybody else enters their own one and sees the name only.
 */
export function AvailabilityUserField({ className }: { className?: string }) {
  const label = useFieldLabels(AVAILABILITY_METADATA);
  const data = useEntityData<AvailabilityDetail>();
  const readOnly = useFormReadOnly();
  return (
    <EntityAutocompleteField
      name="user"
      label={label("user")}
      entity="user"
      disabled={readOnly || data?.userEditable !== true}
      className={className}
    />
  );
}

/**
 * The type, one of the configured ones (`projectforge.availability.types`, `Availability.availableTypes`). Personal
 * data like the internal comment (AvailabilityDao.afterLoad), as the hint tells.
 */
export function AvailabilityTypeField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(AVAILABILITY_METADATA);
  const data = useEntityData<AvailabilityDetail>();
  const readOnly = useFormReadOnly();
  return (
    <SelectField
      name="type"
      label={label("type")}
      hint={t("availability.type.tooltip")}
      disabled={readOnly}
      clearable
      placeholder={t("availability.notSpecified")}
      className={className}
      options={(data?.availableTypes ?? []).map((type) => ({
        value: type,
        label: type,
      }))}
    />
  );
}

/**
 * Where the user works during the period, if it matters (e.g. remote for mobile work): optional, so an empty one
 * is shown as such. Custom for the placeholder, which a declared field doesn't have.
 */
export function AvailabilityLocationField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const label = useFieldLabels(AVAILABILITY_METADATA);
  const readOnly = useFormReadOnly();
  return (
    <SelectField
      name="location"
      label={label("location")}
      hint={t("availability.location.tooltip")}
      disabled={readOnly}
      clearable
      placeholder={t("availability.notSpecified")}
      className={className}
      options={AVAILABILITY_METADATA.fields.location.enumValues.map((v) => ({
        value: v.value,
        label: t(v.i18nKey),
      }))}
    />
  );
}

/**
 * The internal comment, only for the user and HR staff (`Availability.internalCommentVisible`): anybody else
 * isn't sent it (AvailabilityDao.afterLoad), so the field isn't shown at all.
 */
export function AvailabilityInternalCommentField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const label = useFieldLabels(AVAILABILITY_METADATA);
  const data = useEntityData<AvailabilityDetail>();
  const readOnly = useFormReadOnly();
  if (data?.internalCommentVisible !== true) return null;
  return (
    <TextAreaField
      name="internalComment"
      label={label("internalComment")}
      hint={t("availability.internalComment.tooltip")}
      disabled={readOnly}
      rows={3}
      className={className}
    />
  );
}

/**
 * Optional: who stands in for the user during the period — users, also freelancers. A conflict exists only if all of
 * them are away on a day (see AvailabilityConflictNotice). Custom because a collection of references has no field in
 * the metadata.
 */
export function AvailabilitySubstitutesField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <EntityMultiAutocompleteField
      name="substitutes"
      label={t(leafKeyOf("availability.substitutes", t.has))}
      hint={t("availability.substitutes.tooltip")}
      entity="user"
      metadataLess
      className={className}
    />
  );
}
