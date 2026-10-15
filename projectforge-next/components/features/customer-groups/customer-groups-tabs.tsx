"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import type { EditPageTab } from "@/components/shared/edit-page-tabs";
import { HISTORY_TAB_ID } from "@/components/shared/edit/entity-tabs";
import { HistorySection } from "@/components/shared/history/history-section";
import { SectionCard } from "@/components/shared/section-card";
import { SectionHeader } from "@/components/shared/section-header";
import { BusinessUnitList } from "./business-unit-list";
import { GroupList } from "./group-list";

/**
 * REST category of the stored value: the customer groups are one `ConfigurationDO` row, so their
 * history is that row's (`/rs/configuration/history/{id}`).
 */
export const CONFIGURATION_ENTITY = "configuration";

/**
 * The tabs, cards and tab panels of the customer groups page for EditPageShell: one anchor per card,
 * then the change history of the stored row — none before its first save (`id` null).
 */
export function useCustomerGroupsTabs(id: number | null | undefined): {
  tabs: EditPageTab[];
  sections: ReactNode[];
  tabPanels: Record<string, ReactNode>;
} {
  const t = useTranslations();
  const cards = [
    {
      id: "groups",
      label: t("fibu.customerGroups.groups"),
      content: <GroupList />,
    },
    {
      id: "businessUnits",
      label: t("fibu.businessUnits.title"),
      content: <BusinessUnitList />,
    },
  ];
  const tabs: EditPageTab[] = cards.map(({ id, label }) => ({ id, label }));
  const sections = cards.map(({ id, label, content }) => (
    <SectionCard key={id}>
      <SectionHeader title={label} />
      {content}
    </SectionCard>
  ));
  if (id == null) return { tabs, sections, tabPanels: {} };
  return {
    tabs: [
      ...tabs,
      {
        id: HISTORY_TAB_ID,
        label: t("label.historyOfChanges"),
        tab: HISTORY_TAB_ID,
      },
    ],
    sections,
    tabPanels: {
      [HISTORY_TAB_ID]: (
        <HistorySection entity={CONFIGURATION_ENTITY} entityId={id} />
      ),
    },
  };
}
