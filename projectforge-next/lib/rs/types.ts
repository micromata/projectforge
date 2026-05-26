// Mirrors org.projectforge.framework.persistence.api.MagicFilter and
// related Kotlin classes from projectforge-business / projectforge-rest.
// Keep field names aligned with the Spring Boot JSON contract — the
// mock route handler in app/rs/**/list/route.ts and the real backend
// must accept the same payload shape.

export type SortOrder = "ASCENDING" | "DESCENDING";

export interface SortProperty {
  property: string;
  sortOrder: SortOrder;
}

export interface MagicFilterEntryValue {
  value?: string;
  values?: string[];
  id?: number;
  label?: string;
  displayName?: string;
  fromValue?: string;
  toValue?: string;
}

export interface MagicFilterEntry {
  field?: string;
  value?: MagicFilterEntryValue;
  search?: string;
}

export interface MagicFilter {
  entries: MagicFilterEntry[];
  sortProperties: SortProperty[];
  searchString?: string;
  searchHistory?: string;
  deleted?: boolean | null;
  maxRows?: number;
  autoWildcardSearch?: boolean;
  // The Kotlin side reads page size from an entry with field="paginationPageSize".
  // We mirror that, but also accept it at top level for ergonomics on the mock route.
  paginationPageSize?: number;
  // Pragmatic extension: the backend MagicFilter has no native page index. We use
  // `extended.page` for now; later this can be folded into a server-side cursor.
  extended?: Record<string, unknown>;
}

export interface ResultSet<O> {
  resultSet: O[];
  totalSize?: number;
  paginationPageSize?: number;
  resultInfo?: string;
  highlightRowId?: number;
  reloadUI?: boolean;
}
