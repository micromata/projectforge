"use client";

import { useTranslations } from "next-intl";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { FieldHint } from "@/components/shared/form/field-hint";

/**
 * The user's switch whether to see the invoicing quota ("Fakturaquote anzeigen"), with the configured
 * explanation as hint. Shown by the monthly report and the personal statistics; both persist it in the
 * same user pref, so the choice holds on either page.
 */
export function InvoicingQuotaSwitch({
  id,
  checked,
  info,
  onCheckedChange,
}: {
  id: string;
  checked: boolean;
  /** Configured explanation (markdown), or null/undefined for none. */
  info?: string | null;
  onCheckedChange: (checked: boolean) => void;
}) {
  const t = useTranslations();
  const label = t("fibu.monthlyEmployeeReport.showInvoicingQuota");
  return (
    <div className="flex items-center gap-2">
      <Switch id={id} checked={checked} onCheckedChange={onCheckedChange} />
      <Label htmlFor={id}>{label}</Label>
      {info && <FieldHint hint={info} label={label} />}
    </div>
  );
}
