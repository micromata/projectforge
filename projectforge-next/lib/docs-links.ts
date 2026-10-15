// Links into the ProjectForge handbook, mirroring `Constants.WEB_DOCS_*` (projectforge-business).
//
// Duplicated rather than fetched: they are documentation urls of the product, not configuration, and
// the backend hands them out nowhere — Wicket puts them into its pages the same way, from the same
// constants. Kept here as one list so the next page that needs one doesn't spell out an origin again.

const WEB_HOME_PAGE_LINK = "https://projectforge.org";

const WEB_DOCS_LINK = `${WEB_HOME_PAGE_LINK}/docs`;

const WEB_DOCS_USER_GUIDE_LINK = `${WEB_DOCS_LINK}/userguide/`;

/**
 * The handbook's chapter on the full text search — what a search field of an indexed list accepts.
 *
 * `Constants.WEB_DOCS_LINK_HANDBUCH_LUCENE`, behind the help icon of Wicket's structure tree form and
 * of every list page's search field.
 */
export const LUCENE_QUERY_DOCS_URL = `${WEB_DOCS_USER_GUIDE_LINK}#full_indexed_search`;
