import type { VisitorbookValues } from "./visitorbook-schema";
import type { VisitorbookDetail } from "./types";

export function toFormValues(visitor: VisitorbookDetail): VisitorbookValues {
  return {
    id: visitor.id ?? null,
    firstname: visitor.firstname ?? "",
    lastname: visitor.lastname ?? "",
    company: visitor.company ?? null,
    visitortype: visitor.visitortype ?? null,
    contactPersons: visitor.contactPersons ?? [],
    comment: visitor.comment ?? null,
  };
}

export function emptyVisitorbookValues(): VisitorbookValues {
  return toFormValues({ id: null });
}
