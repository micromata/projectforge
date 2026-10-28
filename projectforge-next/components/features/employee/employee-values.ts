import type { EmployeeValues } from "./employee-schema";
import type { EmployeeDetail, EmployeeRef } from "./types";
import { toPickerRef } from "@/lib/page-def/reference";

/** The reference a picker binds to (see toPickerRef). */
function toRef(ref: EmployeeRef | null | undefined) {
  return toPickerRef(ref);
}

export function toFormValues(employee: EmployeeDetail): EmployeeValues {
  return {
    id: employee.id ?? null,
    user: toRef(employee.user),
    kost1: toRef(employee.kost1),
    konto: toRef(employee.konto),
    abteilung: employee.abteilung ?? null,
    position: employee.position ?? null,
    staffNumber: employee.staffNumber ?? null,
    eintrittsDatum: employee.eintrittsDatum ?? null,
    austrittsDatum: employee.austrittsDatum ?? null,
    comment: employee.comment ?? null,
  };
}

export function emptyEmployeeValues(): EmployeeValues {
  return {
    id: null,
    user: null,
    kost1: null,
    konto: null,
    abteilung: null,
    position: null,
    staffNumber: null,
    eintrittsDatum: null,
    austrittsDatum: null,
    comment: null,
  };
}
