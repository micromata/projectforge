"use client";

import { useTranslations } from "next-intl";
import { useEntityData } from "@/components/shared/form/form-context";
import { FormAlert } from "@/components/shared/form-alert";
import { MarkdownText } from "@/components/shared/markdown-text";
import {
  isRichTextHtml,
  RICH_TEXT_MUTED_CLASSES,
  RichText,
} from "@/components/shared/rich-text";
import { leafKeyOf } from "@/lib/leaf-key";
import type { TimesheetDetail } from "../../types";

/**
 * The configured AI-time-savings note, shown below the form — the legacy UILayout's `layoutBelowActions`
 * alert. The backend fills `timeSavingsByAINote` only when the
 * installation tracks AI time savings and a note is configured, so an absent text renders nothing.
 *
 * The note is rich text (edited with RichTextEditor in the configuration). A note written before, as
 * markdown that may carry HTML (the legacy alert rendered it with `remarkGfm` + `rehypeRaw`), is rendered
 * as such until it is saved again in the editor.
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
      {isRichTextHtml(note) ? (
        <RichText html={note} className={RICH_TEXT_MUTED_CLASSES} />
      ) : (
        <MarkdownText
          text={note}
          allowHtml
          className={RICH_TEXT_MUTED_CLASSES}
        />
      )}
    </FormAlert>
  );
}
