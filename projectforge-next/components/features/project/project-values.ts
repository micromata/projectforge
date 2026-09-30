import type { ProjectValues } from "./project-schema";
import type { Kost2ArtSelection, ProjectDetail } from "./types";

/** A cost 2 type with all flags set, whatever the JSON left out (NON_NULL drops nothing here, but a false is cheap to be sure of). */
function toKost2Art(
  art: Kost2ArtSelection
): ProjectValues["kost2Arts"][number] {
  return {
    ...art,
    selected: art.selected ?? false,
    existsAlready: art.existsAlready ?? false,
    active: art.active ?? false,
  };
}

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as
 * `undefined`; every value is normalised here, so no field ever holds `undefined` — which a
 * controlled input would read as "uncontrolled" and the schema as a missing value. The mandatory
 * `name` becomes "" rather than null (an emptied required input keeps "").
 */
export function toFormValues(project: ProjectDetail): ProjectValues {
  return {
    id: project.id ?? null,
    nummer: project.nummer ?? null,
    internKost2_4: project.internKost2_4 ?? null,
    name: project.name ?? "",
    identifier: project.identifier ?? null,
    status: project.status ?? null,
    customer: project.customer ?? null,
    konto: project.konto ?? null,
    task: project.task ?? null,
    projektManagerGroup: project.projektManagerGroup ?? null,
    projectManager: project.projectManager ?? null,
    headOfBusinessManager: project.headOfBusinessManager ?? null,
    salesManager: project.salesManager ?? null,
    description: project.description ?? null,
    kost2Arts: (project.kost2Arts ?? []).map(toKost2Art),
    numberLocked: project.numberLocked ?? false,
    created: project.created ?? null,
  };
}

/**
 * Blank form for a project that doesn't exist yet — only a fallback: a new project is loaded from
 * `/rs/project/edit` like an existing one, because that is where the cost 2 types come from.
 *
 * `nummer` starts empty: the number is the user's to assign, and a proposed 0 would be a valid but
 * unintended one.
 */
export function emptyProjectValues(): ProjectValues {
  return {
    id: null,
    nummer: null,
    internKost2_4: null,
    name: "",
    identifier: null,
    status: null,
    customer: null,
    konto: null,
    task: null,
    projektManagerGroup: null,
    projectManager: null,
    headOfBusinessManager: null,
    salesManager: null,
    description: null,
    kost2Arts: [],
    numberLocked: false,
    created: null,
  };
}
