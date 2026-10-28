"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { POLL_PAGE } from "@/components/features/poll/poll.page";

export default function PollListPage() {
  return <EntityListPage page={POLL_PAGE} />;
}
