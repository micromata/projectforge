"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { Badge } from "@/components/ui/badge";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import type { TeamEventAttendeeStatus } from "../../types";
import type { TeamEventEditValues } from "../team-event-edit-schema";

/**
 * The label key of each status, spelt out in full so the i18n key scanner sees it (a
 * `t(\`…${status}\`)` would be invisible to it, see NextI18nKeyScanner).
 */
const STATUS_KEYS: Record<TeamEventAttendeeStatus, string> = {
  ACCEPTED: "plugins.teamcal.attendee.status.accepted",
  COMPLETED: "plugins.teamcal.attendee.status.completed",
  DECLINED: "plugins.teamcal.attendee.status.declined",
  DELEGATED: "plugins.teamcal.attendee.status.delegated",
  IN_PROCESS: "plugins.teamcal.attendee.status.in_process",
  NEEDS_ACTION: "plugins.teamcal.attendee.status.needs_action",
  TENTATIVE: "plugins.teamcal.attendee.status.tentative",
};

/**
 * The attendees of the event, read-only: name, e-mail and their participation status.
 *
 * They are a snapshot stored with the event (`TeamEventDO.attendeesJson`), taken over from an imported
 * ics file or from the legacy attendee table. ProjectForge neither edits them nor sends invitations; the
 * form only carries them back unchanged, so a save keeps them. Renders nothing for an event without
 * attendees (the page drops the whole card then, see teamEvent.page.tsx).
 */
export function AttendeesSection({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const attendees = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TeamEventEditValues).attendees
  );
  if (!attendees?.length) return null;

  return (
    <ul className={cn("flex flex-col gap-2", className)}>
      {attendees.map((attendee, index) => {
        const name = attendee.name || attendee.email;
        return (
          <li
            key={`${attendee.email ?? ""}-${index}`}
            className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm"
          >
            <span className="font-medium">{name}</span>
            {attendee.name && attendee.email ? (
              <a
                href={`mailto:${attendee.email}`}
                className="text-muted-foreground hover:underline"
              >
                {attendee.email}
              </a>
            ) : null}
            {attendee.status ? (
              <Badge variant="secondary">
                {t(STATUS_KEYS[attendee.status])}
              </Badge>
            ) : null}
          </li>
        );
      })}
    </ul>
  );
}
