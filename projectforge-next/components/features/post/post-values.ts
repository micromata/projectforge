import type { PostausgangValues } from "./postausgang-schema";
import type { PosteingangValues } from "./posteingang-schema";
import type { PostausgangDetail, PosteingangDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`, see types.ts) arrives as `undefined`;
 * every value is normalised here, so no field ever holds `undefined`. The mandatory texts hold "" rather than
 * null (see requiredString).
 */
export function toPosteingangValues(
  entry: PosteingangDetail
): PosteingangValues {
  return {
    id: entry.id ?? null,
    datum: entry.datum ?? null,
    type: (entry.type ?? null) as PosteingangValues["type"],
    absender: entry.absender ?? "",
    person: entry.person ?? null,
    inhalt: entry.inhalt ?? "",
    bemerkung: entry.bemerkung ?? null,
  };
}

export function toPostausgangValues(
  entry: PostausgangDetail
): PostausgangValues {
  return {
    id: entry.id ?? null,
    datum: entry.datum ?? null,
    type: (entry.type ?? null) as PostausgangValues["type"],
    empfaenger: entry.empfaenger ?? "",
    person: entry.person ?? null,
    inhalt: entry.inhalt ?? "",
    absender: entry.absender ?? null,
    bemerkung: entry.bemerkung ?? null,
  };
}

/**
 * Blank forms for an entry that doesn't exist yet. The backend presets today and the type (e-mail for the
 * inbox, letter for the outbox, see `newBaseDO` of the two EntityRests); its `newEntry` answer is what the
 * form is reset onto.
 */
export function emptyPosteingangValues(): PosteingangValues {
  return toPosteingangValues({ id: null });
}

export function emptyPostausgangValues(): PostausgangValues {
  return toPostausgangValues({ id: null });
}
