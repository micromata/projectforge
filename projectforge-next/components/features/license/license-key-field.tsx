"use client";

import { useTranslations } from "next-intl";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { useEntityData } from "@/components/shared/form/form-context";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { leafKeyOf } from "@/lib/leaf-key";
import { LICENSE_METADATA } from "@/lib/metadata/license.generated";
import type { LicenseDetail } from "./types";

/**
 * The license key — for administrators and owners only (`License.keyVisible`). Everyone else reads
 * Wicket's "not visible" text in its place, with the tooltip saying who may see it; the backend sends
 * them no key and keeps the stored one on save (`LicenseEntityRest.transformForDB`).
 */
export function LicenseKeyField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = useFieldLabels(LICENSE_METADATA);
  const ids = useFieldIds();
  const data = useEntityData<LicenseDetail>();

  if (data?.keyVisible !== false) {
    return (
      <TextAreaField
        name="key"
        label={label("key")}
        rows={4}
        className={className}
      />
    );
  }
  return (
    <FieldShell
      name="key"
      label={label("key")}
      readOnly
      hint={t("plugins.licensemanagement.key.notvisible.tooltip")}
      invalid={false}
      errors={[]}
      className={className}
      ids={ids}
    >
      <p id={ids.controlId} className="text-sm text-muted-foreground">
        {/* `_`, since the key has a `tooltip` subkey and so becomes a namespace. */}
        {t(leafKeyOf("plugins.licensemanagement.key.notvisible", t.has))}
      </p>
    </FieldShell>
  );
}
