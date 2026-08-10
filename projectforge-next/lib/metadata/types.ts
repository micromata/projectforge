/**
 * Shape of the field metadata generated from the backend entities.
 *
 * The files next to this one (`*.generated.ts`) are written by
 * `GenerateNextFieldMetadataMain` from `@PropertyInfo` and the JPA `@Column`, merged by
 * `ElementsRegistry.getElementInfo` — the same source Wicket and the UILayout pages read.
 * This file is hand-written; it only declares the contract, never a rule.
 *
 * Consumers derive from the metadata instead of restating it: `lib/validation/from-metadata.ts`
 * builds the Zod pieces, the field components take `required`, `maxLength` and the enum options
 * from here.
 */

/** Mirror of the backend's `UIDataType` (projectforge-rest, org.projectforge.ui.UIDataType). */
export type UIDataTypeName =
  | "AMOUNT"
  | "BOOLEAN"
  | "COST1"
  | "COST2"
  | "CUSTOMIZED"
  | "DATE"
  | "DECIMAL"
  | "EMPLOYEE"
  | "GROUP"
  | "INT"
  | "LONG"
  | "KONTO"
  | "LOCALE"
  | "PASSWORD"
  | "PICTURE"
  | "STRING"
  | "TASK"
  | "TIME"
  | "TIMESTAMP"
  | "TIMEZONE"
  | "USER";

/** One constant of an enum property. `value` is what goes over the wire. */
export type EnumValueMetadata = {
  value: string;
  /** Absent for enums not implementing the backend's `I18nEnum` — then only `value` can be shown. */
  i18nKey?: string;
};

export type FieldMetadata = {
  dataType: UIDataTypeName;
  /** Key for the field label. Absent if the entity declares no `i18nKey` for it. */
  i18nKey?: string;
  /** True if `@PropertyInfo(required = true)` or the column is `NOT NULL`. */
  required: boolean;
  /** Only set for strings — `@Column.length` is preset to 255 for other types as well. */
  maxLength?: number;
  /** A getter without a setter: the backend computes the value, the frontend must not send one. */
  readOnly?: boolean;
  tooltipI18nKey?: string;
  /** Present exactly for enum properties. */
  enumValues?: readonly EnumValueMetadata[];
};

export type EntityMetadata = {
  /** Simple class name of the entity, e.g. `BookDO`. For error messages only. */
  entity: string;
  /**
   * Whether the entity records a change history, so an edit page gets a history tab.
   *
   * `HistoryBaseDaoAdapter.isHistorizable`: `@WithHistory` at the class or anywhere above it —
   * which every `DefaultBaseDO` inherits through `AbstractHistorizableBaseDO`. That is why Kost1DO
   * has one although its own `@WithHistory` is commented out.
   */
  historizable: boolean;
  fields: Readonly<Record<string, FieldMetadata>>;
};
