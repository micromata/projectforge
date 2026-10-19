"use client";

import { FieldHint } from "@/components/shared/form/field-hint";
import { RichText, richTextToPlainText } from "@/components/shared/rich-text";

/**
 * The configured description of the group naming convention (parameter `groupNomenclature`), behind
 * an info icon next to the label of a group's name. Nothing if none is configured.
 */
export function GroupNomenclatureHint({
  description,
  label,
}: {
  description: string | null | undefined;
  label: string;
}) {
  if (!description || !richTextToPlainText(description)) return null;
  return (
    <FieldHint
      hint={description}
      content={<RichText html={description} className="max-w-sm" />}
      label={label}
    />
  );
}
