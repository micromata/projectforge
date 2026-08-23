"use client";

import { useTranslations } from "next-intl";
import { Textarea } from "@/components/ui/textarea";
import { SectionCard } from "@/components/shared/section-card";
import { FieldShell, useFieldIds } from "@/components/shared/form/field-shell";

/**
 * The „Änderungskommentar" of an edit page: why this change was made, kept with the history entry the
 * save produces (`HistoryEntryDO.userComment`, filled from `BaseDTO.historyUserComment`).
 *
 * Below the sections and above the action bar, which is where the server laid out pages put it
 * (`layout.layoutBelowActions`, see LayoutUtils.processEditPage) — it is about the save, not about the
 * entity, so it belongs next to the button and not into one of its cards.
 *
 * Not a field of the form: no entity declares it, no schema validates it, and it is not a value that
 * is loaded and written back — it is written once and travels with that one save (see
 * EntityEditPage). Only entities whose DO implements `HistoryUserCommentSupport` offer it, which the
 * backend answers (see useHistoryCommentSupport).
 */
export function HistoryUserCommentField({
  value,
  onChange,
}: {
  value: string;
  onChange: (next: string) => void;
}) {
  const t = useTranslations();
  const ids = useFieldIds();
  // `._`, because `history.userComment` is also the parent of `history.userComment.info` below: the
  // generator puts a key that is a namespace too under the reserved "_" (see GenerateNextI18nMessagesMain).
  const label = t("history.userComment._");
  return (
    <SectionCard className="py-4">
      <FieldShell
        name="historyUserComment"
        label={label}
        hint={t("history.userComment.info")}
        invalid={false}
        errors={[]}
        ids={ids}
      >
        <Textarea
          id={ids.controlId}
          rows={2}
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
      </FieldShell>
    </SectionCard>
  );
}
