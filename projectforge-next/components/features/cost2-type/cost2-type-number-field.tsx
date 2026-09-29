"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { NumberField } from "@/components/shared/form/number-field";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";
import { useEntityData } from "@/components/shared/form/form-context";
import type { Kost2ArtDetail } from "./types";

/** The cost-2 type number written as it reads everywhere else: two digits, 3 → "03" (Kost2Art.getFormattedId). */
function formatTwoDigits(id: number): string {
  return String(id).padStart(2, "0");
}

/**
 * The two-digit cost-2 type number (`fibu.kost2art.nummer`), which is the primary key.
 *
 * A custom control rather than a plain `{ name: "id" }` field for two reasons: the id's metadata type is
 * `LONG`, which the generic form renders as a text input (a string the `z.number()` schema would reject),
 * and it is assigned once and never changed. So on a stored entry it is not an input at all but a read-only
 * two-digit display ("03", matching the list column and the legacy form); only while the type is new is it
 * a numeric field the user fills in.
 */
export function Cost2TypeNumberField({ className }: { className?: string }) {
  const t = useTranslations();
  const label = t("fibu.kost2art.nummer");
  const data = useEntityData<Kost2ArtDetail>();
  const ids = useFieldIds();
  const isExisting = data?.id != null;

  if (isExisting) {
    // The key is fixed, so it is shown, not edited — as the two-digit string it reads as, taken from the
    // DTO's formatted id and falling back to padding the raw id.
    const shown = data?.formattedId ?? formatTwoDigits(data!.id!);
    return (
      <FieldShell
        label={label}
        readOnly
        invalid={false}
        errors={[]}
        className={className}
        ids={ids}
      >
        <Input id={ids.controlId} value={shown} disabled readOnly />
      </FieldShell>
    );
  }

  return (
    <NumberField
      name="id"
      label={label}
      className={className}
      fractionDigits={0}
      maxDigits={2}
    />
  );
}
