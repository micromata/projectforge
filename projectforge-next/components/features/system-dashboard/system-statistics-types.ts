/**
 * The system statistics (`org.projectforge.rest.SystemStatisticsRest`), delivered section by section:
 * one section per statistics builder of the backend.
 */

export interface SystemStatisticsSection {
  id: string;
}

/** Fill level of an entry, `used` and `max` in bytes or as a count. */
export interface SystemStatisticsGauge {
  used: number;
  max: number;
  unit: "BYTES" | "COUNT";
}

export interface SystemStatisticsEntry {
  id: string;
  /** Title of the card the entry belongs to, a section may consist of several. */
  group: string;
  /** Already translated by the backend. */
  title: string;
  /** Already formatted by the backend. */
  value: string;
  gauge?: SystemStatisticsGauge | null;
}

export interface SystemStatisticsSectionData {
  id: string;
  entries: SystemStatisticsEntry[];
}
