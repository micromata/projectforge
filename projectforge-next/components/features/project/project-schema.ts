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
 * `nummernkreis`, `bereich` and `kostFormatted` are absent: the entity derives them from the project
 * number, and the form shows it live (see ProjectNumberField). `internKost2_4` is absent, too: it is
 * legacy, the local number of an internal project covers all its digits.
 */
export const projectSchema = z.object({
  // null while the project is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  /**
   * The local project number (`ProjektDO.nummer`): per customer of a number range, or of the range without
   * customers. Its bounds depend on the range (the digits of `NumberRange.projectDigits`), which the
   * field enforces (ProjectNumberField); the authority is the backend's check
   * (`fibu.projekt.validation.localNumberOutOfRange`, `numbernotfreeforcustomer`). For a free project
   * the form sets it to the project number.
   */
  nummer: m
    .intField("nummer", { min: 0 })
    .refine((v): boolean => v != null, REQUIRED),
  /**
   * The system-wide unique project number: computed for a range (ProjectNumberField keeps it in line),
   * typed for a free one, where empty means the next free one (`ProjektDao.assignProjektnummer`).
   */
  projektnummer: m.intField("projektnummer", { min: 1 }),
  name: m.requiredString("name"),
  identifier: m.nullableString("identifier"),
  status: m.enumField("status"),
  customer: entityRef,
  konto: entityRef,
  task: m.entityField("task"),
  projektManagerGroup: m.entityField("projektManagerGroup"),
  contactPerson: m.entityField("contactPerson"),
  /**
   * The further contact persons. No metadata: the DTO carries them as user references, the entity as
   * comma separated ids (`ProjektDO.additionalContactUserIds`). An empty list rather than null, which is
   * what the picker holds.
   */
  additionalContacts: z.array(
    z.looseObject({ id: z.number(), displayName: z.string().optional() })
  ),
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
