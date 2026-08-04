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

export interface DynamicUIResponse {
  layout: DynamicLayoutNode[];
  layoutBelowActions?: DynamicLayoutNode[];
  actions?: ActionDef[];
  pageMenu?: DynamicLayoutNode[];
  title?: string;
  translations?: Record<string, string>;
  watchFields?: string[];
  namedContainers?: Record<string, DynamicLayoutNode[]>;
}

export interface DynamicPageResponse {
  ui: DynamicUIResponse;
  data: Record<string, unknown>;
  variables?: Record<string, unknown>;
  validationErrors?: ValidationError[];
  targetType?: string;
  url?: string;
}
