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
  sections: ChangelogSection[];
}

export interface ChangelogNews {
  version: string;
  date: string;
  title: string;
  text: string;
  highlights?: string[];
  /** The release the news is shown above: the newest of its version, set by the generator. */
  releaseId: string;
}

export interface Changelog {
  news: ChangelogNews[];
  releases: ChangelogRelease[];
}
