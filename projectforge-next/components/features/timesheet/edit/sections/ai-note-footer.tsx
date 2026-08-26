"use client";

import { useTranslations } from "next-intl";
import { useEntityData } from "@/components/shared/form/form-context";
import { FormAlert } from "@/components/shared/form-alert";
import { MarkdownText } from "@/components/shared/markdown-text";
import { leafKeyOf } from "@/lib/leaf-key";
import type { TimesheetDetail } from "../../types";

/**
 * The configured AI-time-savings note, shown below the form — the legacy UILayout's `layoutBelowActions`
 * alert (`TimesheetPagesRest.createEditLayout`). The backend fills `timeSavingsByAINote` only when the
 * installation tracks AI time savings and a note is configured, so an absent text renders nothing.
 *
 * The note is authored (from the admin configuration) as markdown that may carry HTML — the legacy alert
 * rendered it with `remarkGfm` + `rehypeRaw` (`DynamicAlert`), so the note's `<a>` link becomes a link
 * and not literal text, hence `allowHtml`.
 */
export function AiNoteFooter() {
  const t = useTranslations();
  const note = useEntityData<TimesheetDetail>()?.timeSavingsByAINote;
  if (!note?.trim()) return null;

  return (
    <FormAlert tone="info">
      {/* Both a text and a namespace in the bundle, so resolve to its exported leaf (see leafKeyOf). */}
      <p className="mb-1 font-semibold">
        {t(leafKeyOf("timesheet.ai.timeSavedByAI", t.has))}
      </p>
      <MarkdownText text={note} allowHtml />
    </FormAlert>
  );
}
