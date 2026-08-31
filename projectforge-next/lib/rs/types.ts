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
  /** Accepted values for a LIST filter. */
  values?: string[];
  /** Referenced entity for an OBJECT filter (history search by user). */
  id?: number;
  label?: string;
  displayName?: string;
  // Range bounds. The Kotlin fields are fromValue/toValue but @JsonProperty
  // renames them on the wire, so these are the names the backend accepts.
  from?: string;
  to?: string;
  /**
   * The art the two bounds were given as, `PeriodKindId` (see lib/date-period.ts). The backend stores and
   * returns it untouched — its search is always built from `from`/`to` — but a period meaning "bis heute"
   * cannot be recognised from its dates and has to be recomputed when the filter comes back (see
   * [periodOfDateValue]).
   *
   * A real field on `MagicFilterEntry.Value`, not something smuggled along: Jackson is configured to drop
   * unknown properties silently (`JacksonConfiguration`), so an invented one would simply be lost on the
   * way through the stored filter.
   */
  periodKind?: string;
}

export interface MagicFilterEntry {
  field?: string;
  value?: MagicFilterEntryValue;
  search?: string;
}

// Only fields the Kotlin MagicFilter can deserialize. It rejects unknown ones
// (no @JsonIgnoreProperties), so anything extra fails the whole request with 400.
// Notably `paginationPageSize` is a computed read-only val there: it is returned
// in ResultSet but must be SENT as an entry with field="paginationPageSize".
export interface MagicFilter {
  entries: MagicFilterEntry[];
  sortProperties: SortProperty[];
  searchString?: string;
  searchHistory?: string;
  deleted?: boolean | null;
  maxRows?: number;
  autoWildcardSearch?: boolean;
  sortAndLimitMaxRowsWhileSelect?: boolean;
  multiSelection?: boolean | null;
  name?: string;
  id?: number;
  extended?: Record<string, unknown>;
}

/**
 * Envelope of the `listPage` call (mirrors `AbstractEntityRest.ListPageRequest`): the filter plus the
 * slice to serve. Offset and limit travel *beside* the filter, never inside it — the filter is the
 * persisted favorite and the argument of `isModified`, so a page flip must not touch it.
 */
export interface ListPageRequest {
  filter: MagicFilter;
  offset: number;
  limit: number;
  /** Skip the session-cached id list and re-materialize it — sent right after the client's own write. */
  refresh?: boolean;
  /**
   * Do not remember this filter as the user's current one (mirrors `ListPageRequest.doNotStore`). Set for a
   * transient jump into a pre-filtered list — the consumption bar linking to a task's time sheets — so the
   * filter does not stay behind when the list is opened from the menu afterwards.
   */
  doNotStore?: boolean;
}

export const PAGINATION_PAGE_SIZE_FIELD = "paginationPageSize";

/**
 * Page size travels as a filter entry, mirroring MagicFilter.paginationPageSize.
 *
 * In `values`, not `value`: the Kotlin getter reads `value.values[0]` and only falls back to
 * `parseInteger("${entry.value}")` — the *object's* toString, never `value.value`. A size sent as
 * `value` is therefore dropped, and the fallback logs a "Can't parse integer:
 * MagicFilterEntry$Value@1a2b3c" warning on every list request.
 */
export function paginationPageSizeEntry(pageSize: number): MagicFilterEntry {
  return {
    field: PAGINATION_PAGE_SIZE_FIELD,
    value: { values: [String(pageSize)] },
  };
}

export interface ResultSet<O> {
  resultSet: O[];
  /**
   * For a server-side paged result (`listPage`) this is the size of the *whole* result, not of the
   * page in `resultSet` — what the footer's total and the page count are read from. For the plain
   * `list` call it is the size of the returned list.
   */
  totalSize?: number;
  /** The page's offset into the whole result, echoed back by `listPage`; absent for a plain `list`. */
  offset?: number;
  /** The page size `listPage` served; absent for a plain `list`. */
  limit?: number;
  /**
   * Whether `totalSize` is the exact count or a lower bound — false once the id list hit `maxRows` and
   * was truncated (see `DBIdResult.truncated`). Always true for a plain `list`.
   */
  totalSizeExact?: boolean;
  /**
   * Whether the result was capped by the row limit, so more rows match the filter than came back. The
   * typed counterpart of the truncation note in `resultInfo` (see `ResultSet.resultSetTruncated` on the
   * backend): a hand built list renders its own red warning from this rather than the server's markup.
   */
  resultSetTruncated?: boolean;
  paginationPageSize?: number;
  resultInfo?: string;
  /**
   * Aggregates over the whole result set, in the shape the entity's rest class defines
   * (`OrderEntityRest.OrderStatistics` for the order book) — see `ResultSet.statistics` there.
   *
   * `unknown` because the shape belongs to the entity: the page that declares the component rendering
   * it is the one place that knows the type, and narrows there.
   */
  statistics?: unknown;
  highlightRowId?: number;
  reloadUI?: boolean;
}

// --- Authentication & User Status ---

export interface UserData {
  username: string;
  organization?: string;
  fullname?: string;
  firstName?: string;
  lastName?: string;
  userId: number;
  employeeId?: number;
  locale: string;
  timeZone: string;
  dateFormat: string;
  dateFormatShort: string;
  timestampFormatMinutes: string;
  timestampFormatSeconds: string;
  timestampFormatMillis: string;
  jsDateFormat: string;
  jsDateFormatShort: string;
  jsTimestampFormatMinutes: string;
  jsTimestampFormatSeconds: string;
  firstDayOfWeek: string;
  firstDayOfWeekSunday0: number;
  isoFirstDayOfWeekValue: number;
  timeNotation: string;
  currency: string;
  thousandSeparator: string;
  decimalSeparator: string;
}

/**
 * Application facts carried by the authenticated `userStatus` (SystemStatusRest.SystemData).
 * Unlike the public `/rsPublic/systemStatus` (see {@link SystemStatus}), this one is not masked —
 * it holds the real version and build date, which is why the status bar reads them from here.
 */
export interface SystemData {
  version: string;
  buildTimestamp: string;
  buildDate: string;
  releaseYear: string;
  copyRightYears: string;
}

/**
 * One JIRA server as the client builds browse links against: an issue key is appended to {@link baseUrl}
 * (see {@link buildJiraIssueUrl}), and {@link projects} are the key prefixes hosted on it.
 */
export interface JiraServerConfig {
  baseUrl: string;
  projects: string[];
}

/**
 * The JIRA configuration `userStatus` carries so the client can turn issue keys into links itself,
 * mirroring the backend's `JiraUtils` — absent (Spring's `JsonInclude.NON_NULL`) where JIRA is not
 * configured. Pick the server whose {@link JiraServerConfig.projects} the key starts with, else fall
 * back to {@link defaultBrowseBaseUrl} (see lib/jira.ts).
 */
export interface JiraConfig {
  configured: boolean;
  defaultBrowseBaseUrl?: string;
  servers?: JiraServerConfig[];
}

export interface UserStatus {
  userData: UserData;
  systemData?: SystemData;
  alertMessage?: string;
  /** CSRF token of the session, see setCsrfToken in ./client.ts. */
  csrfToken?: string;
  /**
   * Whether the user is in the admin group. This app declares its menus itself, so it has to decide
   * on its own whether to offer an admin only action (see ListGearMenu).
   */
  adminUser?: boolean;
  /** JIRA config for client-side issue linking, only where JIRA is configured (see JiraConfig). */
  jira?: JiraConfig;
}

export interface SystemStatus {
  appname: string;
  version: string;
  buildTimestamp: string;
  buildDate: string;
  releaseYear: string;
  copyRightYears: string;
  logoUrl?: string;
  /** Optional dark-mode variant of {@link logoUrl}; absent when no dark logo is configured. */
  logoUrlDark?: string;
  /** `projectforge.development.mode` of the instance — the flag behind DevelopmentMarker. */
  developmentMode?: boolean;
  setupRedirectUrl?: string;
  messageOfTheDay?: string;
}

// --- Menu ---

export interface MenuBadge {
  counter?: number;
  style?: string;
  tooltip?: string;
}

/**
 * What the client is meant to do with a menu entry's url (see MenuItemTargetType). Absent means
 * `REDIRECT` — the backend only sends the field where it is something else.
 */
export type MenuItemTargetType = "REDIRECT" | "MODAL" | "DOWNLOAD" | "RESTCALL";

export interface MenuItem {
  id?: string;
  title: string;
  url?: string;
  /** Unique across the whole menu, unlike `id`, whose dots the backend replaces for the DOM. */
  key?: string;
  badge?: MenuBadge;
  subMenu?: MenuItem[];
  type?: MenuItemTargetType;
}

export interface Menu {
  menuItems: MenuItem[];
  badge?: MenuBadge;
}

export interface MenuData {
  mainMenu: Menu;
  favoritesMenu: Menu;
  myAccountMenu: Menu;
  /**
   * The entries the user opened last, most recent first, resolved out of the three menus above
   * (RecentMenuEntriesService). Optional, so a build against an older backend degrades to an empty
   * history instead of breaking.
   */
  recentMenu?: Menu;
}

// --- Dynamic Layout ---

export interface DynamicLayoutNode {
  type: string;
  key: string;
  content?: DynamicLayoutNode[];
  // Type-specific props are passed through
  [prop: string]: unknown;
}

/**
 * A filter field the backend offers for a list page. Delivered inside the
 * `searchFilter` named container of a list layout, built by
 * LayoutListFilterUtils.createNamedSearchFilterContainer from the DAO's search
 * fields — so the set differs per entity.
 */
export type FilterType =
  | "STRING"
  | "DATE"
  | "TIMESTAMP"
  | "BOOLEAN"
  | "OBJECT"
  | "LIST";

export interface FilterListValue {
  id: string;
  displayName: string;
}

export interface FilterElement {
  id: string;
  key: string;
  type: "FILTER_ELEMENT";
  filterType: FilterType;
  label?: string;
  /** LIST: the values to choose from. */
  values?: FilterListValue[];
  /** LIST: whether several values may be selected. */
  multi?: boolean;
  /** OBJECT: endpoint for looking up entities while typing. */
  autoCompletion?: {
    minChars?: number;
    type?: string;
    url?: string;
  };
  /** TIMESTAMP: allows an open-ended range. */
  openInterval?: boolean;
  /** TIMESTAMP: quick range presets (YEAR, MONTH, WEEK, DAY, UNTIL_NOW). */
  selectors?: string[];
  /**
   * The backend asks for this filter to stay visible on the list page, even without a value
   * (UIFilterElement.defaultFilter — e.g. the address list's "only my addresses").
   */
  defaultFilter?: boolean;
  /** Explains a cryptic field name; the only place the backend can do so. */
  tooltip?: string;
  /**
   * Translated label of the group the field belongs to, from its parent chain ("Kunde" for
   * `kunde.name`); absent for a field of the entity itself. See [buildFilterGroups].
   */
  group?: string;
  /** The field's label without the group prefix ("Name"); only set where `group` is. */
  shortLabel?: string;
  /**
   * A field the entity indexes but never declares (no `@PropertyInfo`, so `label` is the raw property
   * name, e.g. `attachmentsIds`) — searchable, but not a field a user came for.
   */
  technical?: boolean;
}

export interface ValidationError {
  fieldId: string;
  message: string;
}

/** Lower-case on the wire, see the @JsonProperty names of org.projectforge.ui.UIColor. */
export type UIColorName =
  | "danger"
  | "dark"
  | "info"
  | "light"
  | "link"
  | "primary"
  | "secondary"
  | "success"
  | "warning";

/** A button of the action group or of the layout (org.projectforge.ui.UIButton). */
export interface ActionDef {
  id: string;
  title?: string;
  color?: UIColorName;
  outline?: boolean;
  default?: boolean;
  tooltip?: string;
  disabled?: boolean;
  confirmMessage?: string;
  responseAction?: ResponseAction;
}

/** org.projectforge.ui.TargetType. */
export type TargetType =
  | "REDIRECT"
  | "DOWNLOAD"
  | "UPDATE"
  | "GET"
  | "PUT"
  | "POST"
  | "DELETE"
  | "MODAL"
  | "CLOSE_MODAL"
  | "RELOAD"
  | "NOTHING"
  | "TOAST"
  | "CHECK_AUTHENTICATION";

/** org.projectforge.ui.ResponseAction.Message - the text is already translated by the backend. */
export interface ResponseActionMessage {
  i18nKey?: string;
  message?: string;
  technicalMessage?: string;
  color?: UIColorName;
}

/** What every action endpoint answers: how the client should proceed. */
export interface ResponseAction {
  targetType?: TargetType;
  url?: string;
  /** UPDATE/CLOSE_MODAL: merge the payload into the current state instead of replacing it. */
  merge?: boolean;
  validationErrors?: ValidationError[];
  message?: ResponseActionMessage;
  variables?: Record<string, unknown>;
}

/**
 * Opaque state the server keeps per edit page (csrf token, return-to-caller); the client only
 * echoes it back - see rest/dto/ServerData.kt.
 */
export interface ServerData {
  csrfToken?: string;
  returnToCaller?: string;
  returnToCallerParams?: Record<string, string>;
}

/** Body of save/update/delete/cancel/watchFields - see rest/dto/PostData.kt. */
export interface PostData<D = Record<string, unknown>> {
  data: D;
  watchFieldsTriggered?: string[];
  serverData?: ServerData;
}

export interface AutoCompletion {
  /** Number of characters before the lookup fires; UIInput/UISelect default it to 2. */
  minChars?: number;
  values?: { value: unknown; label: string; allSearchableFields?: string }[];
  /** USER, EMPLOYEE, GROUP, CUSTOMER, PROJECT - set when the field stores whole entities. */
  type?: string;
  /** Contains a literal ":search" placeholder to substitute. */
  url?: string;
  urlParams?: Record<string, string>;
}

/** One offered option of a UISelect (org.projectforge.ui.UISelectValue). */
export interface UISelectValue {
  id: unknown;
  displayName: string;
}

export interface NamedContainer {
  id: string;
  type: string;
  content?: DynamicLayoutNode[];
}

export interface DynamicUIResponse {
  /** Identifies this layout instance; used to keep dom ids unique (UILayout.uid). */
  uid?: string;
  layout: DynamicLayoutNode[];
  layoutBelowActions?: DynamicLayoutNode[];
  actions?: ActionDef[];
  pageMenu?: DynamicLayoutNode[];
  title?: string;
  /**
   * The same page in the legacy React app, e.g. `react/book/edit/42` — the escape hatch shown next
   * to the title while the migration runs (see LegacyPageLink). Absent for pages with no legacy
   * counterpart.
   */
  legacyUrl?: string;
  translations?: Record<string, string>;
  watchFields?: string[];
  // An array, not a map: each container carries its own id (e.g. "searchFilter").
  namedContainers?: NamedContainer[];
}

/** One saved filter, mirroring Favorites.FavoriteIdTitle. */
export interface FavoriteIdTitle {
  id: number;
  name: string;
}

/**
 * What `{entity}/initialList` and the `filter/*` endpoints return
 * (AbstractPagesRest.InitialListData).
 *
 * Beyond the layout it carries the filter state the backend keeps per user: the
 * current filter (stored on every list call, see saveCurrentFilter) and the list
 * of saved filters.
 */
export interface InitialListData extends DynamicPageResponse {
  filter?: MagicFilter;
  filterFavorites?: FavoriteIdTitle[];
  standardEditPage?: string;
  /**
   * The legacy edit page with `:id` for the id, e.g. `react/book/edit/:id` or `wa/cost1Edit?id=:id`.
   * Read by the hand built edit pages, which have no `{entity}/edit` response of their own to take
   * `ui.legacyUrl` from (see LegacyPageLink).
   */
  legacyEditPage?: string;
  /**
   * The legacy page for adding an entry, e.g. `react/book/edit` or `wa/cost1Edit`. Its own field,
   * because dropping the id from `legacyEditPage` is a per-app rule (path segment vs. query
   * parameter), not a suffix cut.
   */
  legacyNewEntryPage?: string;
  quickSelectUrl?: string;
  useModalEditDialog?: boolean;
}

/**
 * What `{entity}/listMeta` returns for a hand built list page (`rest/core/ListMetaData.kt`):
 * everything the page needs beside its rows.
 *
 * The layout free counterpart of {@link InitialListData} — no `ui`, no translations and no result
 * set, because the page renders itself and fetches its rows from `list`.
 */
export interface ListMetaData {
  /** The filter the user left this page with, restored from their user prefs. */
  filter?: MagicFilter;
  filterFavorites?: FavoriteIdTitle[];
  /**
   * The saved filter `filter` is based on, with its values — absent while the current filter comes from
   * no favorite. It is what tells an edited filter from an unchanged one after a reload, when nothing in
   * this session has applied or written a favorite (see [useFilterFavorites]).
   */
  filterFavorite?: MagicFilter;
  /**
   * The filter fields of this entity, derived by the backend from the DAO's search fields — which is
   * why they can't be declared in the frontend.
   */
  filterElements?: FilterElement[];
  /** Url template of the page a row leads to, with `:id` for the id. */
  standardEditPage?: string;
  /**
   * The way back to the legacy list page, e.g. `wa/orderBookList`. Absent for a page whose legacy
   * counterpart is gone (see LegacyPageLink).
   */
  legacyListPage?: string;
  /** The legacy edit page with `:id` for the id, e.g. `wa/orderBookEdit?id=:id`. */
  legacyEditPage?: string;
  /**
   * The legacy page for adding an entry. Its own field, because dropping the id from
   * `legacyEditPage` is a per-app rule (path segment vs. query parameter), not a suffix cut.
   */
  legacyNewEntryPage?: string;
  userAccess?: UserAccess;
  /**
   * The entries this user last ticked for a mass update, so entering the selection mode after a
   * reload — or after a detour through the legacy app — comes back to what was picked.
   *
   * Lives in the HTTP session for 60 minutes (`MultiSelectionSupport.SessionContext`), under the
   * same key `{page}/select` writes; absent while nothing is selected. It restores the *ticks*, not
   * the mode: the page decides whether it is in selection mode (see useListSelection).
   */
  selectedIds?: number[];
  variables?: Record<string, unknown>;
}

/**
 * What the logged-in user may do with an entity (`UILayout.UserAccess`). Every flag is optional:
 * Spring omits the ones the backend left null (`JsonInclude.NON_NULL`).
 */
export interface UserAccess {
  insert?: boolean;
  update?: boolean;
  delete?: boolean;
  /** Whether the change history may be seen. */
  history?: boolean;
  /** Whether history entries may be commented (`BaseDao.supportsHistoryUserComments`). */
  editHistoryComments?: boolean;
  /**
   * Whether the user may see the entity's entries at all — the one flag here that is more than a hint:
   * false means every read is refused, so there is no page to show (see useReadAccessGuard).
   */
  read?: boolean;
}

/**
 * Answer of `filter/create|rename|update|delete`: a map holding whichever of the
 * two values the endpoint touched. `filter/update` returns an empty map.
 */
export interface FilterFavoritesResponse {
  filter?: MagicFilter;
  filterFavorites?: FavoriteIdTitle[];
}

/**
 * Layout and data in one - see rest/dto/FormLayoutData.kt.
 *
 * The same shape doubles as the answer of an action endpoint, which is why it carries the
 * ResponseAction fields as well (the backend writes both into one JSON object).
 */
export interface DynamicPageResponse extends ResponseAction {
  ui: DynamicUIResponse;
  data: Record<string, unknown>;
  serverData?: ServerData;
}
