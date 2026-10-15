"use client";

import { useTranslations } from "next-intl";
import { Textarea } from "@/components/ui/textarea";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";

export interface ContributionMarginRemarkProps {
  value: string | null;
  /** An emptied remark is written as null, so the stored JSON carries no empty string. */
  onChange: (remark: string | null) => void;
  onBlur: () => void;
}

/**
 * The remark of the contribution margin settings: how the calculated rate is derived, why a setting was
 * changed. Part of the parameter's JSON, so its changes show in the parameter's history.
 */
export function ContributionMarginRemark({
  value,
  onChange,
  onBlur,
}: ContributionMarginRemarkProps) {
  const t = useTranslations();
  const ids = useFieldIds();
  return (
    <FieldShell label={t("comment")} invalid={false} errors={[]} ids={ids}>
      <Textarea
        id={ids.controlId}
        rows={4}
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value || null)}
        onBlur={onBlur}
      />
    </FieldShell>
  );
}
