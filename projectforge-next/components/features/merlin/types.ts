// The DTO itself (org.projectforge.plugins.merlin.MerlinTemplate) is declared in lib/rs/merlin.ts, beside the
// calls of the form and the execution page.
import type { MerlinTemplate } from "@/lib/rs/merlin";

export type MerlinDetail = MerlinTemplate;

/** Projection the list renders — the same DTO, with the id the table keys rows by. */
export type MerlinListRow = MerlinTemplate & { id: number };
