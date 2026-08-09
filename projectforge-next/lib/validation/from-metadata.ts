/**
 * Builds the Zod pieces of a hand-written form from the generated field metadata, so a rule is
 * declared in the backend entity and nowhere else.
 *
 * Every rule comes from `lib/metadata/*.generated.ts`, i.e. from `@PropertyInfo` and the JPA
 * `@Column` of the entity (see `lib/metadata/types.ts`). What stays hand-written is which fields a
 * form has and how they are shaped — the DTOs don't have the field set of their DO — but not
 * *whether* a field is mandatory, how long its value may be, or which values an enum has.
 *
 * The invariants of the form's values are the ones the field components rely on: a mandatory string
 * is never null (an emptied input keeps ""), an optional one is null when blank, and an enum stays
 * nullable even when mandatory so a missing value can be reported instead of silently defaulted.
 */

import { z } from "zod";
import type { EntityMetadata, FieldMetadata } from "@/lib/metadata/types";
import { REQUIRED, maxLengthMarker } from "./markers";

type FieldsOf<M extends EntityMetadata> = M["fields"];
type FieldName<M extends EntityMetadata> = keyof FieldsOf<M> & string;

/** The fields the generator gave an `enumValues` list — the only ones [enumField] accepts. */
type EnumFieldName<M extends EntityMetadata> = {
  [K in FieldName<M>]: FieldsOf<M>[K] extends { enumValues: unknown }
    ? K
    : never;
}[FieldName<M>];

/** The constants of an enum field as a union of string literals, e.g. `"PRESENT" | "MISSED" | …`. */
type EnumValueOf<
  M extends EntityMetadata,
  K extends FieldName<M>,
> = FieldsOf<M>[K] extends { enumValues: readonly { value: infer V }[] }
  ? V
  : never;

/** Translates a key of the backend bundle, i.e. `useTranslations()` without a namespace. */
type Translate = (key: string) => string;

export interface SelectOption {
  value: string;
  label: string;
}

export function fromMetadata<M extends EntityMetadata>(metadata: M) {
  /**
   * Throws rather than falling back: a name the metadata doesn't know is a field that was renamed in
   * the entity, and silently treating it as optional is how the two drift apart again. A typo is
   * caught by `tsc` already — the parameter types are unions of the generated field names.
   */
  function field(name: FieldName<M>): FieldMetadata {
    const meta = metadata.fields[name];
    if (!meta) {
      throw new Error(
        `${metadata.entity} has no field "${name}" — regenerate lib/metadata (DevelopmentMainForRelease).`
      );
    }
    return meta;
  }

  /** Only strings carry a limit; see `FieldMetadata.maxLength`. */
  function maxLengthOf(name: FieldName<M>): number | undefined {
    return field(name).maxLength;
  }

  /**
   * Mandatory text field: never null, an emptied input holds "" (see InputField). `refine` rather
   * than `min`/`max` so the issue carries our marker instead of one of Zod's untranslated English
   * defaults.
   */
  function requiredString(name: FieldName<M>) {
    const maxLength = maxLengthOf(name);
    const schema = z.string().refine((v) => v.trim().length > 0, REQUIRED);
    return maxLength === undefined
      ? schema
      : schema.refine((v) => v.length <= maxLength, maxLengthMarker(maxLength));
  }

  /**
   * Optional text field. `nullable`, not `nullish`: this validates the *form's* values, and those
   * never hold `undefined` — a field Spring omitted from the JSON (`JsonInclude.Include.NON_NULL`)
   * is normalised to null before it reaches the form. Widening it here would make the schema's input
   * type wider than the form's values, which @tanstack/react-form rejects.
   */
  function nullableString(name: FieldName<M>) {
    const maxLength = maxLengthOf(name);
    const base =
      maxLength === undefined
        ? z.string()
        : z
            .string()
            .refine((v) => v.length <= maxLength, maxLengthMarker(maxLength));
    return base
      .nullable()
      .transform((v) => (v && v.trim().length > 0 ? v : null));
  }

  /**
   * Enum field, restricted to the constants of the backend enum. Stays `nullable` even when
   * mandatory: a value the entity doesn't have must be reportable as missing rather than replaced by
   * the first constant. The `: boolean` matters — an inferred type guard would narrow the field to
   * non-null in the schema's type, and the form's values (which do allow null) would no longer match.
   */
  function enumField<K extends EnumFieldName<M>>(name: K) {
    const values = enumValues(name).map((v) => v.value);
    const base = z
      .enum(values as [string, ...string[]])
      .nullable() as unknown as z.ZodType<
      EnumValueOf<M, K> | null,
      EnumValueOf<M, K> | null
    >;
    return field(name).required
      ? base.refine((v): boolean => v != null, REQUIRED)
      : base;
  }

  /**
   * The options of an enum field, in the order the backend enum declares them, labelled with the
   * `i18nKey` of each constant (`I18nEnum.i18nKey`, the same the legacy pages show). A constant
   * without a key — an enum not implementing `I18nEnum` — falls back to its name; inventing a text
   * here would be a second place to maintain one.
   */
  function enumOptions(name: EnumFieldName<M>, t: Translate): SelectOption[] {
    return enumValues(name).map((v) => ({
      value: v.value,
      label: v.i18nKey ? t(v.i18nKey) : v.value,
    }));
  }

  function enumValues(name: FieldName<M>) {
    const values = field(name).enumValues;
    if (!values) {
      throw new Error(
        `${metadata.entity}.${name} is no enum property — it has no enumValues in the generated metadata.`
      );
    }
    return values;
  }

  return { field, requiredString, nullableString, enumField, enumOptions };
}
