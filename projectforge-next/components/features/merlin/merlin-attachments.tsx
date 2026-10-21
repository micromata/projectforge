"use client";

import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { AttachmentList } from "@/components/shared/attachments/attachment-list";
import { AttachmentServiceProvider } from "@/components/shared/attachments/attachment-service";
import { useFormReadOnly } from "@/components/shared/form/form-context";
import {
  DEFAULT_ATTACHMENT_SERVICE,
  type AttachmentService,
} from "@/lib/rs/attachments";
import { analyzeMerlinTemplate } from "@/lib/rs/merlin";
import { toast } from "@/lib/toast";
import { useMerlinVariables } from "./use-merlin-variables";

/** As in the backend (`MerlinAttachmentsActionListener`): no encryption of a template. */
const MERLIN_ATTACHMENT_SERVICE: AttachmentService = {
  ...DEFAULT_ATTACHMENT_SERVICE,
  encryption: false,
};

/**
 * The attachments of the template: its Word® template (the latest docx) and optionally an Excel® template
 * definition (the latest xlsx); an older one of the same kind is renamed to a backup by the backend. After
 * every change the Word® template is analyzed again (`merlin/analyze`), so variables found in a new template
 * show up in the form at once — next to the unsaved changes, which are kept.
 */
export function MerlinAttachments({ id }: { id: number | null }) {
  const t = useTranslations();
  const readOnly = useFormReadOnly();
  const { currentTemplate, apply } = useMerlinVariables();
  const analysis = useMutation({
    mutationFn: () => analyzeMerlinTemplate(currentTemplate()),
    onSuccess: (result) => apply(result, true),
    onError: () => toast.error(t("errorpage.title")),
  });
  return (
    <AttachmentServiceProvider service={MERLIN_ATTACHMENT_SERVICE}>
      <AttachmentList
        entity="merlin"
        id={id}
        embedded
        readOnly={readOnly}
        onChanged={() => analysis.mutate()}
      />
    </AttachmentServiceProvider>
  );
}
