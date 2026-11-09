// Links into the ProjectForge handbook, mirroring `Constants.WEB_DOCS_*` (projectforge-business).
//
// Duplicated rather than fetched: they are documentation urls of the product, not configuration, and
// the backend hands them out nowhere — Wicket puts them into its pages the same way, from the same
// constants. Kept here as one list so the next page that needs one doesn't spell out an origin again.

const WEB_HOME_PAGE_LINK = "https://projectforge.org";

/**
 * The handbook in the language of the user: the German docs live below `/de` (with the anchors of the
 * English ones, see `site/README.adoc`), every other language gets the English docs.
 */
function webDocsLink(locale: string): string {
  return locale === "de"
    ? `${WEB_HOME_PAGE_LINK}/de/docs`
    : `${WEB_HOME_PAGE_LINK}/docs`;
}

/**
 * The handbook's chapter on the full text search — what a search field of an indexed list accepts.
 *
 * `Constants.WEB_DOCS_LINK_HANDBUCH_LUCENE` (English only), behind the help icon of Wicket's structure
 * tree form and of every list page's search field.
 */
export function luceneQueryDocsUrl(locale: string): string {
  return `${webDocsLink(locale)}/userguide/#full_indexed_search`;
}

/** The user guide's chapter on the libraries of a CVE-Radar component: the formats, a script collecting them in a zip. */
export function cveRadarLibrariesDocsUrl(locale: string): string {
  return `${webDocsLink(locale)}/userguide/#cve_radar_libraries`;
}

/** The user guide on the bulk import of SBOMs into an area (`[[cve_radar_sbom_import]]`). */
export function cveRadarSbomImportDocsUrl(locale: string): string {
  return `${webDocsLink(locale)}/userguide/#cve_radar_sbom_import`;
}
