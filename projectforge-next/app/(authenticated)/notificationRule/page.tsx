"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { NOTIFICATION_RULE_PAGE } from "@/components/features/notification-rule/notification-rule.page";

export default function NotificationRuleListPage() {
  return <EntityListPage page={NOTIFICATION_RULE_PAGE} />;
}
