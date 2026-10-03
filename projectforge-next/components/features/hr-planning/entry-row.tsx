"use client";

import { useTranslations } from "next-intl";
import { CollapsibleSummary } from "@/components/shared/collapsible-summary";
import { EntityAutocompleteField } from "@/components/shared/form/entity-autocomplete-field";
import { NestedFieldMetadata } from "@/components/shared/form/form-context";
import { NumberField } from "@/components/shared/form/number-field";
import { RepeatableRow } from "@/components/shared/form/repeatable-row";
import { SelectField } from "@/components/shared/form/select-field";
import { TextAreaField } from "@/components/shared/form/text-area-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { JiraIssuesLinks } from "@/components/shared/jira/jira-issues-links";
import { useJiraFieldHint } from "@/components/shared/jira/use-jira-field-hint";
import { useFormatContext } from "@/hooks/use-format";
import { formatNumber } from "@/lib/format";
import { leafKeyOf } from "@/lib/leaf-key";
import { HR_PLANNING_ENTRY_METADATA } from "@/lib/metadata/hr-planning-entry.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { HRPlanningEntryValues } from "./hr-planning-schema";
import { entryTotalHours } from "./hr-planning-values";

const e = fromMetadata(HR_PLANNING_ENTRY_METADATA);

/** The hour fields of an entry in the order of the legacy form: unassigned, Monday to Friday, weekend. */
export const HOUR_FIELDS = [
  "unassignedHours",
  "mondayHours",
  "tuesdayHours",
  "wednesdayHours",
  "thursdayHours",
  "fridayHours",
  "weekendHours",
] as const;

/** The probabilities the legacy form offered (`HRPlanningEditTablePanel`), in percent. */
const PROBABILITIES = [25, 50, 75, 95, 100];

export interface HRPlanningEntryRowProps {
  entry: HRPlanningEntryValues;
  /** Prefix of every field name of this row, e.g. `entries[1].`. */
  prefix: string;
  /** Position of the entry in the week, from 1 — the header's handle of a row that has no project yet. */
  number: number;
  onRemove?: () => void;
  /** Takes a soft-deleted entry back — see [RepeatableRow], which renders the deleted state. */
  onRestore?: () => void;
}

/**
 * One entry of a planned week: a project *or* a status (absence, illness, leave, other), how sure and
 * how important it is, the hours per day and what is done.
 *
 * The header is a [CollapsibleSummary]: project or status and the sum of the hours identify the entry,
 * the rest is shown only while the row is folded.
 */
export function HRPlanningEntryRow({
  entry,
  prefix,
  number,
  onRemove,
  onRestore,
}: HRPlanningEntryRowProps) {
  const t = useTranslations();
  const label = useFieldLabels(HR_PLANNING_ENTRY_METADATA);
  const format = useFormatContext();
  const jiraHint = useJiraFieldHint(true);
  const name = (field: string) => `${prefix}${field}`;
  const statusOption = e
    .enumOptions("status", t)
    .find((option) => option.value === entry.status);
  const what = entry.projekt?.displayName ?? statusOption?.label;
  const total = entryTotalHours(entry);

  return (
    <NestedFieldMetadata
      metadata={HR_PLANNING_ENTRY_METADATA}
      namePrefix={prefix}
    >
      <RepeatableRow
        header={
          <CollapsibleSummary
            primary={
              <>
                <span className="shrink-0 text-muted-foreground">{number}</span>
                {what && <span className="truncate font-medium">{what}</span>}
                {/* Right-hand end of the line, as the sum of an order position is. */}
                <span className="ml-auto shrink-0 tabular-nums">
                  {formatNumber(total, format, 2)} {t("hr.planning.hours")}
                </span>
              </>
            }
            details={[
              entry.priority &&
                e
                  .enumOptions("priority", t)
                  .find((o) => o.value === entry.priority)?.label,
              entry.probability != null && `${entry.probability}%`,
              entry.description,
            ]}
          />
        }
        defaultOpen={entry.id == null}
        deleted={entry.deleted}
        onRemove={onRemove}
        onRestore={onRestore}
        removeLabel={what ?? `#${number}`}
      >
        {/* A project or a status, never both — `HRPlanningEntityRest.validate` reports a violation at the
            project field. */}
        <EntityAutocompleteField
          name={name("projekt")}
          label={t(leafKeyOf("fibu.projekt", t.has))}
          entity="project"
          metadataLess
        />
        <SelectField
          name={name("status")}
          label={label("status")}
          options={e.enumOptions("status", t)}
        />
        <div className="grid grid-cols-2 gap-3">
          <SelectField
            name={name("priority")}
            label={label("priority")}
            options={e.enumOptions("priority", t)}
          />
          <SelectField
            name={name("probability")}
            label={t(leafKeyOf("hr.planning.probability", t.has))}
            options={PROBABILITIES.map((value) => ({
              value: String(value),
              label: `${value}%`,
            }))}
            valueType="number"
          />
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-7 md:col-span-3">
          {HOUR_FIELDS.map((field) => (
            <NumberField
              key={field}
              name={name(field)}
              label={label(field)}
              fractionDigits={2}
            />
          ))}
        </div>
        <TextAreaField
          name={name("description")}
          label={label("description")}
          hint={jiraHint}
          rows={3}
          className="md:col-span-3"
        />
        <JiraIssuesLinks text={entry.description} className="md:col-span-3" />
      </RepeatableRow>
    </NestedFieldMetadata>
  );
}
