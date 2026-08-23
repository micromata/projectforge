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
import {
  INTEGER,
  REQUIRED,
  maxLengthMarker,
  maxMarker,
  minMarker,
} from "./markers";

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
   * The bounds of a numeric field: `@PropertyInfo(min = …, max = …)` of the entity, which the backend
   * enforces itself (`ValidationUtils.validateFields`) — declared on the property, so the form only
   * spares the user a round trip instead of stating a second rule.
   *
   * `overrides` is for the ranges not (yet) on the entity: a cost number's segments, which the
   * `SegmentedNumberField` needs per segment anyway (see `cost-number-segments.ts`). What the entity
   * declares wins, so a range moved into the annotation cannot be silently contradicted here.
   */
  function bounds(
    name: FieldName<M>,
    overrides: { min?: number; max?: number }
  ): { min?: number; max?: number } {
    const meta = field(name);
    return {
      min: meta.min ?? overrides.min,
      max: meta.max ?? overrides.max,
    };
  }

  function withBounds<T extends z.ZodType<number | null, number | null>>(
    schema: T,
    { min, max }: { min?: number; max?: number }
  ): z.ZodType<number | null, number | null> {
    let result: z.ZodType<number | null, number | null> = schema;
    if (min !== undefined) {
      result = result.refine((v) => v == null || v >= min, minMarker(min));
    }
    if (max !== undefined) {
      result = result.refine((v) => v == null || v <= max, maxMarker(max));
    }
    return result;
  }

  /**
   * Whole-number field, bounded by what the entity declares (see [bounds]).
   *
   * `nullable`, and mandatory means "not null" rather than "not zero": an emptied box must be
   * reportable as missing instead of silently saving a 0, which would be a different, valid number.
   */
  function intField(
    name: FieldName<M>,
    overrides: { min?: number; max?: number } = {}
  ) {
    let schema: z.ZodType<number | null, number | null> = z
      .number()
      .nullable()
      .refine((v) => v == null || Number.isInteger(v), INTEGER);
    if (field(name).required) {
      schema = schema.refine((v): boolean => v != null, REQUIRED);
    }
    return withBounds(schema, bounds(name, overrides));
  }

  /**
   * Decimal field — an amount, a quantity of person days: a `BigDecimal` of the entity, which travels
   * as a JSON number and is held as one. Bounded by what the entity declares, as [intField].
   *
   * No digit count: `@Column(precision, scale)` is not carried by the generated metadata, and a scale
   * is a rounding rule rather than a validation one — the backend rounds what it stores. `nullable`
   * for the same reason as [intField]: an emptied box must be reportable as missing instead of
   * silently saving a 0, which is a different, valid amount.
   */
  function decimalField(
    name: FieldName<M>,
    overrides: { min?: number; max?: number } = {}
  ) {
    let schema: z.ZodType<number | null, number | null> = z.number().nullable();
    if (field(name).required) {
      schema = schema.refine((v): boolean => v != null, REQUIRED);
    }
    return withBounds(schema, bounds(name, overrides));
  }

  /**
   * Boolean field, never null: these are Kotlin primitives in the entities
   * (`AuftragsPositionDO.vollstaendigFakturiert`), so "not set" is not a value the backend can hold —
   * and `required` on one of them would mean "must be true", which no entity means by it.
   */
  function booleanField(name: FieldName<M>) {
    // Only to make a name the entity doesn't have fail here as it does everywhere else.
    field(name);
    return z.boolean();
  }

  /**
   * A referenced entity as the DTO carries it: `{id, displayName}` and whatever else the backend sent,
   * of which only the id is written back (`BaseDTO.copyTo` resolves the object by id).
   *
   * `passthrough`, so a field the backend adds later is not stripped from a value that is only being
   * handed back. Mandatory means "not null", the same as for the other non-string kinds.
   */
  function entityField(name: FieldName<M>) {
    const schema = z
      .looseObject({ id: z.number(), displayName: z.string().optional() })
      .nullable();
    return field(name).required
      ? schema.refine((v): boolean => v != null, REQUIRED)
      : schema;
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

  return {
    field,
    requiredString,
    nullableString,
    intField,
    decimalField,
    booleanField,
    entityField,
    enumField,
    enumOptions,
  };
}
