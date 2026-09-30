import { z } from "zod";
import { PROJEKT_METADATA } from "@/lib/metadata/projekt.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

/**
 * Every rule below — mandatory, maximum length, the constants of the status enum — comes from ProjektDO
 * through `lib/metadata/projekt.generated.ts`. Hand-written are only what the metadata cannot describe:
 * the customer and the account (foreign DOs without `UIDataType`), the cost 2 types (a list of the DTO
 * only), and the bounds of the number parts (see below).
 */
const m = fromMetadata(PROJEKT_METADATA);

/** A reference as the DTO carries it: the id is what `copyTo` resolves the object by. */
const entityRef = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable();

/** One cost 2 type of the form (see Kost2ArtSelection); only `selected` is edited. */
const kost2Art = z.looseObject({
  id: z.number(),
  selected: z.boolean(),
  existsAlready: z.boolean(),
  active: z.boolean(),
});

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.Project — a hand-written decision.
 *
 * `nummernkreis`, `bereich` and `kostFormatted` are absent: the entity derives them from the customer
 * (or the internal range) and the number, and the form shows them live (see ProjectNumberField).
 */
export const projectSchema = z.object({
  // null while the project is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  /**
   * The project's two digits of the cost number, 0..99 (`ProjektDO.nummer`, a two-digit part); the
   * metadata carries no bounds. Unique within the customer or internal range — the backend's check
   * (`fibu.projekt.validation.numbernotfreeforcustomer`).
   */
  nummer: m
    .intField("nummer", { min: 0, max: 99 })
    .refine((v): boolean => v != null, REQUIRED),
  /**
   * The internal range of a project without customer ("4.xxx"), three digits. Only required when a cost
   * 2 unit is to be created without customer — a cross-field rule the backend checks.
   */
  internKost2_4: m.intField("internKost2_4", { min: 0, max: 999 }),
  name: m.requiredString("name"),
  identifier: m.nullableString("identifier"),
  status: m.enumField("status"),
  customer: entityRef,
  konto: entityRef,
  task: m.entityField("task"),
  projektManagerGroup: m.entityField("projektManagerGroup"),
  projectManager: m.entityField("projectManager"),
  headOfBusinessManager: m.entityField("headOfBusinessManager"),
  salesManager: m.entityField("salesManager"),
  description: m.nullableString("description"),
  kost2Arts: z.array(kost2Art),
  /** Read-only: number and customer are fixed, see ProjectNumberField. Ignored by the backend on save. */
  numberLocked: z.boolean(),
  created: z.string().nullable(),
});

export type ProjectValues = z.infer<typeof projectSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors) instead of vanishing into a field nobody sees.
 */
export const PROJECT_FIELDS = Object.keys(
  projectSchema.shape
) as readonly (keyof ProjectValues)[];
