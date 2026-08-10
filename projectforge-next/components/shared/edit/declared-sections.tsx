"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { cn } from "@/lib/utils";
import type { EntityMetadata, FieldMetadata } from "@/lib/metadata/types";
import { labelKeyFor } from "@/lib/page-def/define-page";
import type { FieldDeclaration, SectionDef } from "@/lib/page-def/types";

const SPAN_CLASS = { 1: undefined, 2: "md:col-span-2", 3: "md:col-span-3" };

/**
 * One card of the edit page, rendered from its declaration: order, grouping, width and label.
 *
 * Which component a field gets follows its data type — a date goes through the shared DateInput, an
 * enum becomes a select of its own constants, a field declaring `rows` a textarea. `required`,
 * `maxLength` and the enum's values are never passed in: the field components read them from the
 * metadata in the form context, the same source the Zod schema reads.
 */
export function DeclaredSection<M extends EntityMetadata>({
  section,
  metadata,
  id,
}: {
  section: SectionDef<M>;
  metadata: M;
  /** id of the entity being edited, null while adding — what a `render` body needs. */
  id: number | null;
}) {
  const t = useTranslations();
  return (
    <SectionCard>
      <SectionHeader title={t(section.titleKey)} />
      {section.render ? (
        section.render({ id })
      ) : (
        <div className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3">
          {section.fields?.map((field) => (
            <DeclaredFormField
              key={"name" in field ? field.name : field.custom.name}
              field={field}
              metadata={metadata}
            />
          ))}
        </div>
      )}
    </SectionCard>
  );
}

function DeclaredFormField<M extends EntityMetadata>({
  field,
  metadata,
}: {
  field: FieldDeclaration<M>;
  metadata: M;
}): ReactNode {
  const t = useTranslations();
  const className = cn(SPAN_CLASS[field.span ?? 1]);

  if ("custom" in field) {
    const Custom = field.custom;
    return <Custom className={className} />;
  }

  const meta: FieldMetadata = metadata.fields[field.name] ?? {
    dataType: "STRING",
    required: false,
  };
  const translate = t as unknown as ((key: string) => string) & {
    has: (key: string) => boolean;
  };
  const label = translate(
    labelKeyFor(metadata, field.name, translate.has, field.labelKey)
  );
  const hint = field.hintKey ? translate(field.hintKey) : undefined;
  const common = { name: field.name, label, hint, className };

  if (meta.enumValues) {
    return (
      <SelectField
        {...common}
        emphasized={field.emphasized}
        options={meta.enumValues.map((v) => ({
          value: v.value,
          label: v.i18nKey ? translate(v.i18nKey) : v.value,
        }))}
      />
    );
  }
  if (field.rows) {
    return <TextAreaField {...common} rows={field.rows} />;
  }
  return (
    <InputField {...common} type={meta.dataType === "DATE" ? "date" : "text"} />
  );
}
