"use client";

import { InputField } from "@/components/shared/form/input-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { RichText, richTextToPlainText } from "@/components/shared/rich-text";
import { useGroupNomenclature } from "@/hooks/use-group-nomenclature";
import { GROUP_METADATA } from "@/lib/metadata/group.generated";

/**
 * The name of a group, with the configured naming convention (parameter `groupNomenclature`) as its
 * hint. A custom field because the hint is configured rich text, not an i18n key; without a
 * description it is the plain declared field.
 */
export function GroupNameField({ className }: { className?: string }) {
  const label = useFieldLabels(GROUP_METADATA);
  const description = useGroupNomenclature()?.description;
  const hint =
    description && richTextToPlainText(description) ? (
      <RichText html={description} className="max-w-sm" />
    ) : undefined;
  return (
    <InputField
      name="name"
      label={label("name")}
      hint={hint}
      className={className}
    />
  );
}
