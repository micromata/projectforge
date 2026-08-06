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

export const PAGINATION_PAGE_SIZE_FIELD = "paginationPageSize";

/** Page size travels as a filter entry, mirroring MagicFilter.paginationPageSize. */
export function paginationPageSizeEntry(pageSize: number): MagicFilterEntry {
  return {
    field: PAGINATION_PAGE_SIZE_FIELD,
    value: { value: String(pageSize) },
  };
}

export interface ResultSet<O> {
  resultSet: O[];
  totalSize?: number;
  paginationPageSize?: number;
  resultInfo?: string;
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

export interface UserStatus {
  userData: UserData;
  systemData?: Record<string, unknown>;
  alertMessage?: string;
}

export interface SystemStatus {
  appname: string;
  version: string;
  buildTimestamp: string;
  buildDate: string;
  releaseYear: string;
  copyRightYears: string;
  logoUrl?: string;
  setupRedirectUrl?: string;
  messageOfTheDay?: string;
}

// --- Menu ---

export interface MenuBadge {
  counter?: number;
  style?: string;
  tooltip?: string;
}

export interface MenuItem {
  id?: string;
  title: string;
  url?: string;
  key?: string;
  badge?: MenuBadge;
  subMenu?: MenuItem[];
}

export interface Menu {
  menuItems: MenuItem[];
  badge?: MenuBadge;
}

export interface MenuData {
  mainMenu: Menu;
  favoritesMenu: Menu;
  myAccountMenu: Menu;
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
}

export interface ValidationError {
  fieldId: string;
  message: string;
}

export interface ActionDef {
  id: string;
  title?: string;
  style?: string;
  type?: string;
  url?: string;
  responseAction?: ResponseAction;
  confirmMessage?: string;
}

export interface ResponseAction {
  targetType: string;
  url?: string;
  message?: string;
  variables?: Record<string, unknown>;
}

export interface NamedContainer {
  id: string;
  type: string;
  content?: DynamicLayoutNode[];
}

export interface DynamicUIResponse {
  layout: DynamicLayoutNode[];
  layoutBelowActions?: DynamicLayoutNode[];
  actions?: ActionDef[];
  pageMenu?: DynamicLayoutNode[];
  title?: string;
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
  quickSelectUrl?: string;
  useModalEditDialog?: boolean;
}

/**
 * Answer of `filter/create|rename|update|delete`: a map holding whichever of the
 * two values the endpoint touched. `filter/update` returns an empty map.
 */
export interface FilterFavoritesResponse {
  filter?: MagicFilter;
  filterFavorites?: FavoriteIdTitle[];
}

export interface DynamicPageResponse {
  ui: DynamicUIResponse;
  data: Record<string, unknown>;
  variables?: Record<string, unknown>;
  validationErrors?: ValidationError[];
  targetType?: string;
  url?: string;
}
