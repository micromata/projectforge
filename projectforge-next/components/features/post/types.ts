// Mirrors org.projectforge.business.orga.PosteingangDO and PostausgangDO: both are transferred as they are,
// without a DTO (PosteingangEntityRest, PostausgangEntityRest are AbstractDOEntityRest). Keep the field names
// in sync with the entities.

/**
 * Every optional property is `?`, not just `| null`: Spring's mapper uses `JsonInclude.Include.NON_NULL`
 * (JacksonConfiguration), so an empty field is absent from the JSON rather than null. `toFormValues`
 * normalises that away.
 */
interface PostDetail {
  /** null for an entry that has not been saved yet (Spring assigns the id). */
  id: number | null;
  /** ISO date (yyyy-MM-dd). */
  datum?: string | null;
  /** `PostType` (BRIEF, E_MAIL, …). */
  type?: string | null;
  absender?: string | null;
  person?: string | null;
  inhalt?: string | null;
  bemerkung?: string | null;
  created?: string | null;
  lastUpdate?: string | null;
}

/** An inbox entry (Posteingang). */
export type PosteingangDetail = PostDetail;

/** An outbox entry (Postausgang): the receiver in addition. */
export interface PostausgangDetail extends PostDetail {
  empfaenger?: string | null;
}

/** Projection the inbox list renders — the same entity, with the id the table keys rows by. */
export interface PosteingangListRow extends PosteingangDetail {
  id: number;
}

/** Projection the outbox list renders — the same entity, with the id the table keys rows by. */
export interface PostausgangListRow extends PostausgangDetail {
  id: number;
}
