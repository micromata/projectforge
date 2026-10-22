"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  FieldShell,
  useFieldIds,
  type FieldIds,
} from "@/components/shared/form/field-shell";

/**
 * A labelled field of the booking grant dialog. No field errors: the backend's refusal is one message, shown
 * above the fields.
 */
export function BookingGrantField({
  label,
  required,
  children,
}: {
  label: string;
  required?: boolean;
  children: (ids: FieldIds) => ReactNode;
}) {
  const ids = useFieldIds();
  return (
    <FieldShell
      label={label}
      required={required}
      invalid={false}
      errors={[]}
      ids={ids}
    >
      {children(ids)}
    </FieldShell>
  );
}

/**
 * Whether the grant is an invitation link, which takes effect once its user accepts it.
 */
export function BookingGrantInvitationCheckbox({
  checked,
  onChange,
}: {
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  const t = useTranslations();
  return (
    <HintTooltip text={t("task.bookingGrant.asInvitation.tooltip")}>
      <div className="flex items-center gap-2">
        <Checkbox
          id="booking-grant-invitation"
          checked={checked}
          onCheckedChange={(value) => onChange(value === true)}
        />
        <Label htmlFor="booking-grant-invitation">
          {t("task.bookingGrant.asInvitation._")}
        </Label>
      </div>
    </HintTooltip>
  );
}
