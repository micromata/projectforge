import { z } from "zod";
import { VISITORBOOK_METADATA } from "@/lib/metadata/visitorbook.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";

const m = fromMetadata(VISITORBOOK_METADATA);

/** A contact person, as `Visitorbook.contactPersons` carries it: the id is what `copyTo` stores. */
const employeeRef = z.looseObject({
  id: z.number(),
  displayName: z.string().nullable().optional(),
});

export const visitorbookSchema = z.object({
  // null while the visitor is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  firstname: m.requiredString("firstname"),
  lastname: m.requiredString("lastname"),
  company: m.nullableString("company"),
  visitortype: m.enumField("visitortype"),
  /**
   * No metadata: a collection of employees (`VisitorbookDO.contactPersons`). An empty list rather than null,
   * which is what the picker holds.
   */
  contactPersons: z.array(employeeRef),
  comment: m.nullableString("comment"),
});

export type VisitorbookValues = z.infer<typeof visitorbookSchema>;

export const VISITORBOOK_FIELDS = Object.keys(
  visitorbookSchema.shape
) as readonly (keyof VisitorbookValues)[];
