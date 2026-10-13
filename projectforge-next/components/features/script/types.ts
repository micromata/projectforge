// The DTO itself (org.projectforge.rest.dto.Script) is declared in lib/rs/script.ts, beside the calls of
// the execution page that answer it as well.
import type { Script } from "@/lib/rs/script";

export type ScriptDetail = Script;

/** Projection the list pages render — the same DTO, with the id the table keys rows by. */
export type ScriptListRow = Script & { id: number };
