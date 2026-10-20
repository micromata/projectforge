"use client";

import { useTranslations } from "next-intl";
import { InputField } from "@/components/shared/form/input-field";
import { TextAreaField } from "@/components/shared/form/text-area-field";

/**
 * Subject and text of the invitation mail, sent when the poll is saved for the first time. Not stored: the
 * DTO only carries them for that save (`Poll.invitationSubject`), so the section is shown for a new poll only.
 */
export function PollInvitationSubjectField({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  return (
    <InputField
      name="invitationSubject"
      label={t("poll.invitation.subject._")}
      hint={t("poll.invitation.subject.tooltip")}
      className={className}
      metadataLess
    />
  );
}

export function PollInvitationTextField({ className }: { className?: string }) {
  const t = useTranslations();
  return (
    <TextAreaField
      name="invitationText"
      label={t("poll.invitation.text._")}
      hint={t("poll.invitation.text.tooltip")}
      rows={5}
      className={className}
      metadataLess
    />
  );
}
