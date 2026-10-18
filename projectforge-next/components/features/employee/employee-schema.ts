import { z } from "zod";
import { EMPLOYEE_METADATA } from "@/lib/metadata/employee.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { REQUIRED } from "@/lib/validation/markers";

const m = fromMetadata(EMPLOYEE_METADATA);

const ref = z
  .looseObject({ id: z.number(), displayName: z.string().optional() })
  .nullable();

export const employeeSchema = z.object({
  // null while the employee is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  // Mandatory: an employee is always one of the users (EmployeeDao refuses an employee without one).
  user: ref.refine((v): boolean => v != null, REQUIRED),
  kost1: ref,
  konto: ref,
  abteilung: m.nullableString("abteilung"),
  position: m.nullableString("position"),
  staffNumber: m.nullableString("staffNumber"),
  eintrittsDatum: m.instantField("eintrittsDatum"),
  austrittsDatum: m.instantField("austrittsDatum"),
  comment: m.nullableString("comment"),
});

export type EmployeeValues = z.infer<typeof employeeSchema>;

export const EMPLOYEE_FIELDS = Object.keys(
  employeeSchema.shape
) as readonly (keyof EmployeeValues)[];
