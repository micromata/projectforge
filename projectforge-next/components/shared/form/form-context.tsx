"use client";

import { createContext, useContext } from "react";
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
}

const Ctx = createContext<EntityEditFormContext | null>(null);

export const EntityEditFormProvider = Ctx.Provider;

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
 */
export function useFieldMetadata(name: string): FieldMetadata {
  const { metadata } = useFormContext();
  const meta = metadata.fields[name];
  if (!meta && name !== "id" && process.env.NODE_ENV !== "production") {
    console.warn(
      `${metadata.entity} has no field "${name}" — treating it as an optional string. ` +
        "Check the name, or regenerate lib/metadata (DevelopmentMainForRelease)."
    );
  }
  return meta ?? { dataType: "STRING", required: false };
}
