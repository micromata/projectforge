"use client";

import { createContext, useContext, useMemo, type ReactNode } from "react";
import type { EntityMetadata, FieldMetadata } from "@/lib/metadata/types";

/**
 * The form object of @tanstack/react-form.
 *
 * This is the one deliberately untyped value of the shared form layer. Naming the real type means
 * spelling out the full generics tuple of `useForm` (values, and one type parameter per validator
 * slot), which no consumer can write down and which would have to be threaded through every field
 * component. Instead the public props of every component in this folder are fully typed, and nothing
 * outside `components/shared/form/` ever sees this `any`.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type EntityForm = any;

interface EntityEditFormContext {
  form: EntityForm;
  /**
   * The generated metadata of the entity being edited, e.g. `BOOK_METADATA`. Carried in the context
   * rather than imported by the field components, so they work for any entity.
   */
  metadata: EntityMetadata;
  /**
   * Prefix the field names below carry over their name in [metadata] — `positionen[2].` for a row of a
   * nested collection. Set by [NestedFieldMetadata], absent on the form itself.
   */
  namePrefix?: string;
  /**
   * Whether the whole form is shown and not edited — an entry marked as deleted, which offers nothing
   * but its restore (see EntityEditPage).
   *
   * The fieldset around the sections is what actually blocks the input; this is what makes the fields
   * *look* it. Native disabling is inherited by every control inside, but it is invisible to React, so a
   * field could neither drop its clear button nor mark its label read-only without being told.
   */
  readOnly?: boolean;
  /**
   * The loaded entity as the backend returned it — the DTO behind the form values, not the values.
   *
   * For a custom field that has to look at something the user does not edit: a time sheet's `tags` are
   * the choices its `tag` select offers and whether it is shown at all (see TagField), server-set on the
   * DTO like `timeSavingsByAIEnabled` and absent from the form values. `undefined` on a new entry before
   * its preset loads (and on a form that carries none).
   */
  data?: unknown;
}

const Ctx = createContext<EntityEditFormContext | null>(null);

export const EntityEditFormProvider = Ctx.Provider;

/**
 * Scopes the field metadata to a nested entity: inside, `positionen[2].titel` is looked up as `titel`
 * of the *position's* metadata.
 *
 * Without this every field of a row would miss in the order's metadata and silently fall back to
 * "optional string" — losing `required`, `maxLength` and the enum constants, which is precisely the
 * drift `useFieldMetadata` warns about. The form itself stays the one above; only what the fields are
 * validated and labelled against changes.
 *
 * @param namePrefix The **full** path prefix of the row, not one relative to an enclosing provider:
 *   `positionen[1].kostZuweisungen[0].` for a cost assignment of the invoice form's second nesting
 *   level. This provider *replaces* the context rather than extending it, and `useFieldMetadata` strips
 *   exactly **one** prefix off the field name — so a relative prefix would leave `positionen[1].` in
 *   front of `kost1` and the lookup would miss.
 */
export function NestedFieldMetadata({
  metadata,
  namePrefix,
  children,
}: {
  metadata: EntityMetadata;
  namePrefix: string;
  children: ReactNode;
}) {
  const { form, readOnly, data } = useFormContext();
  const value = useMemo(
    () => ({ form, metadata, namePrefix, readOnly, data }),
    [form, metadata, namePrefix, readOnly, data]
  );
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

function useFormContext(): EntityEditFormContext {
  const ctx = useContext(Ctx);
  if (!ctx) {
    throw new Error(
      "The shared form fields must be used inside an EntityEditFormProvider."
    );
  }
  return ctx;
}

export function useEntityEditForm(): EntityForm {
  return useFormContext().form;
}

/**
 * Whether every field of this form is read-only, whatever its own declaration says — see
 * [EntityEditFormContext.readOnly].
 */
export function useFormReadOnly(): boolean {
  return useFormContext().readOnly === true;
}

/**
 * The loaded entity behind the form — for a custom field that reads something the user does not edit
 * (see [EntityEditFormContext.data]). The caller names the DTO type; it is not validated here.
 */
export function useEntityData<T = unknown>(): T | undefined {
  return useFormContext().data as T | undefined;
}

/**
 * The whole metadata object, for a component that has to look at several fields at once (see
 * SegmentedNumberField, which needs the `required` flag of every one of its boxes).
 */
export function useEntityMetadata(): EntityMetadata {
  return useFormContext().metadata;
}

/**
 * The backend's rules for one field of the form — `required`, `maxLength`, the enum constants — as
 * generated from the entity (see lib/metadata/*.generated.ts).
 *
 * The field components read them from here instead of taking them as props, so the schema
 * (lib/validation/from-metadata.ts) and what the input actually allows cannot drift apart: both read
 * the same object.
 *
 * A name the metadata doesn't know falls back to a neutral optional string rather than throwing —
 * `id` is a legitimate case, being the DTO's identity and not an editable field. Because a typo would
 * look exactly the same, it is logged in development: a field silently turning into "optional string"
 * is how the form and the entity drifted apart before.
 *
 * @param metadataLess Silences that warning for a field the metadata *cannot* carry, whatever the
 * entity declares: an order's customer and project are `KundeDO`/`ProjektDO`, for which there is no
 * `UIDataType`, so `ElementsRegistry` never reports them (see UIDataTypeUtils). Only for those — a
 * field that is merely missing is drift, and the warning is what makes it visible.
 */
export function useFieldMetadata(
  name: string,
  metadataLess = false
): FieldMetadata {
  const { metadata, namePrefix } = useFormContext();
  // Inside a row of a nested collection the form name carries the path (`positionen[2].titel`), while
  // the metadata knows the plain property — so the prefix comes off before the lookup.
  const plain =
    namePrefix && name.startsWith(namePrefix)
      ? name.slice(namePrefix.length)
      : name;
  const meta = metadata.fields[plain];
  if (
    !meta &&
    !metadataLess &&
    plain !== "id" &&
    process.env.NODE_ENV !== "production"
  ) {
    console.warn(
      `${metadata.entity} has no field "${plain}" — treating it as an optional string. ` +
        "Check the name, or regenerate lib/metadata (DevelopmentMainForRelease)."
    );
  }
  return meta ?? { dataType: "STRING", required: false };
}
