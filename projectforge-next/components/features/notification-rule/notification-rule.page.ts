import { NOTIFICATION_RULE_METADATA } from "@/lib/metadata/notification-rule.generated";
import { definePage } from "@/lib/page-def/define-page";
import { DeliveryStepsField } from "./delivery-steps-field";
import { MenuBadgeField } from "./menu-badge-field";
import { NotificationRuleListActions } from "./notification-rule-list-actions";
import {
  notificationRuleSchema,
  NOTIFICATION_RULE_ARRAY_FIELDS,
  NOTIFICATION_RULE_FIELDS,
  type NotificationRuleValues,
} from "./notification-rule-schema";
import {
  emptyNotificationRuleValues,
  toFormValues,
} from "./notification-rule-values";
import {
  AllEmployeesField,
  EditableByGroupsField,
  EmployeeStatusField,
  ExcludedEmployeeStatusField,
  OnlyAffectedField,
  RecipientGroupsField,
  RecipientUsersField,
} from "./recipient-fields";
import { RuleActions } from "./rule-actions";
import { RuleLogTab } from "./rule-log-tab";
import { RuleParamsField } from "./rule-params-field";
import { RuleTextField } from "./rule-text-field";
import { ScheduleField } from "./schedule-field";
import type { NotificationRuleDetail, NotificationRuleListRow } from "./types";

export const NOTIFICATION_RULE_LIST_QUERY_KEY = ["notificationRule"] as const;

/**
 * The rules of the notification system (NotificationRuleEntityRest), for admins and the finance group.
 * Most of a rule is kept as JSON by NotificationRuleDO, so most fields are custom ones without metadata.
 */
export const NOTIFICATION_RULE_PAGE = definePage<
  NotificationRuleListRow,
  NotificationRuleValues,
  NotificationRuleDetail,
  typeof NOTIFICATION_RULE_METADATA
>({
  entity: "notificationRule",
  metadata: NOTIFICATION_RULE_METADATA,
  route: "/notificationRule",
  queryKey: NOTIFICATION_RULE_LIST_QUERY_KEY,
  categoryKey: "menu.administration",
  titleKey: "notification.rule.title",
  defaultSort: { id: "name" },
  listActions: NotificationRuleListActions,
  columns: [
    { name: "name", size: 260 },
    { name: "ruleType", size: 200 },
    { name: "active", size: 90 },
    { name: "severity", size: 110 },
    { name: "display", size: 170 },
    {
      id: "lastRun",
      labelKey: "notification.rule.lastRun",
      accessor: (row) => row.lastRun,
      dataType: "TIMESTAMP",
      size: 130,
    },
    { name: "description", size: 300 },
    { name: "lastUpdate", size: 130 },
  ],
  edit: {
    schema: notificationRuleSchema,
    fieldNames: NOTIFICATION_RULE_FIELDS,
    arrayFieldNames: NOTIFICATION_RULE_ARRAY_FIELDS,
    defaultValues: emptyNotificationRuleValues,
    toFormValues,
    title: (rule) => rule.name ?? "",
    newTitleKey: "notification.rule.newTitle",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "notification.rule.sections.general",
        headerActions: RuleActions,
        fields: [
          { name: "name", span: 3 },
          { name: "active" },
          { name: "ruleType", span: 2 },
          { custom: RuleParamsField, span: 2 },
          { custom: ScheduleField, span: 4 },
          { name: "description", span: 4, rows: 2 },
        ],
      },
      {
        id: "recipients",
        titleKey: "notification.rule.recipients",
        fields: [
          { custom: RecipientGroupsField, span: 2 },
          { custom: RecipientUsersField, span: 2 },
          { custom: AllEmployeesField, span: 2 },
          { custom: OnlyAffectedField, span: 2 },
          { custom: EmployeeStatusField, span: 4 },
          { custom: ExcludedEmployeeStatusField, span: 4 },
        ],
      },
      {
        id: "delivery",
        titleKey: "notification.rule.delivery",
        fields: [
          { custom: DeliveryStepsField, span: 4 },
          { name: "severity" },
          { name: "display" },
          { custom: MenuBadgeField, span: 2 },
          { name: "manualDone", span: 4 },
        ],
      },
      {
        id: "message",
        titleKey: "notification.rule.sections.message",
        fields: [
          { name: "subject", span: 4 },
          { custom: RuleTextField, span: 4 },
        ],
      },
      {
        id: "access",
        titleKey: "notification.rule.sections.access",
        fields: [{ custom: EditableByGroupsField, span: 4 }],
      },
    ],
    extraTabs: [
      { id: "log", labelKey: "notification.log", component: RuleLogTab },
    ],
  },
});
