/**
 * The changelog as GenerateChangelogMain writes it to `lib/generated/changelog.json` — the shape of
 * `changelog/changelog.json`, the single source of the website's changelog and of this page.
 */
export type ChangelogItem = string | { title: string; items: string[] };

export interface ChangelogSection {
  /** A key of `site/_data/tags.yml` (added, improved, fixed, …), validated by the generator. */
  type: string;
  items: ChangelogItem[];
}

export interface ChangelogRelease {
  id: string;
  version: string;
  date: string;
  title: string;
  /** Git tag of a release; a snapshot has the commit range instead. */
  tag?: string;
  fromCommit?: string;
  toCommit?: string;
  intro?: string[];
  /** Level 1: a few keywords and the important fixes, always shown. */
  summary?: string[];
  /** Level 2: about one line per topic, unfolded on request. */
  overview?: string[];
  /** Level 3 (with the intro): all changes, unfolded on request. */
  sections: ChangelogSection[];
}

/**
 * A major version, grouped by the generator: the release opening it (none before the first one), its updates and
 * the snapshots of develop leading up to it, both newest first.
 */
export interface ChangelogGroup {
  major: string;
  head?: ChangelogRelease;
  updates: ChangelogRelease[];
  snapshots: ChangelogRelease[];
}

export interface Changelog {
  /** The entries of changelog/unreleased/, not released yet. */
  unreleased?: ChangelogRelease;
  groups: ChangelogGroup[];
}
