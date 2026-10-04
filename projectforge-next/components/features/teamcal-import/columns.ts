import type { ImportColumn } from "@/components/shared/import/import-types";

/**
 * The preview columns of the ICS import. The period and the recurrence are formatted by the backend
 * (`TeamEventImportDTO`: date only for all-day events, in the user's time zone otherwise), so they are
 * plain text here; every column can change against a stored event of the same uid, hence `diff`.
 */
export const TEAMCAL_IMPORT_COLUMNS: ImportColumn[] = [
  {
    field: "subject",
    headerKey: "plugins.teamcal.event.subject",
    kind: "text",
    diff: true,
    width: 240,
  },
  {
    field: "period",
    headerKey: "timePeriod",
    kind: "text",
    diff: true,
    width: 220,
  },
  {
    field: "location",
    headerKey: "plugins.teamcal.event.location",
    kind: "text",
    diff: true,
    width: 180,
  },
  {
    field: "recurrence",
    headerKey: "plugins.teamcal.event.recurrence",
    kind: "text",
    diff: true,
    width: 180,
  },
  {
    field: "note",
    headerKey: "plugins.teamcal.event.note",
    kind: "text",
    diff: true,
    width: 280,
  },
];
