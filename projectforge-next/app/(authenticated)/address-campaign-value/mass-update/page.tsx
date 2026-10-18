"use client";

import { MassUpdatePage } from "@/components/shared/list/mass-update-page";
import { SelectedEntriesPanel } from "@/components/shared/list/selected-entries-panel";
import { ADDRESS_CAMPAIGN_VALUE_PAGE } from "@/components/features/address-campaign-value/address-campaign-value.page";
import { AddressCampaignValueAdoptMassUpdateField } from "@/components/features/address-campaign-value/address-campaign-value-adopt-mass-update-field";
import type { AddressCampaignValueStatistics } from "@/components/features/address-campaign-value/types";

/**
 * Reached from the list, never linked directly: the selection this changes lives in the HTTP session, and the
 * list put it there before it routed here (see MassUpdatePage).
 */
export default function AddressCampaignValueMassUpdatePage() {
  const massUpdate = ADDRESS_CAMPAIGN_VALUE_PAGE.massUpdate!;
  return (
    <MassUpdatePage
      entity={ADDRESS_CAMPAIGN_VALUE_PAGE.entity}
      massUpdate={massUpdate}
      listRoute={ADDRESS_CAMPAIGN_VALUE_PAGE.route}
      listQueryKey={ADDRESS_CAMPAIGN_VALUE_PAGE.queryKey}
      // Adopting the values of the comparison campaign, declared by the backend only while the list has one
      // (AddressCampaignValueMultiSelectedPageRest.ADOPT_COMPARISON): no entity property behind it.
      customFields={{
        adoptComparison: (setParam, meta) => (
          <AddressCampaignValueAdoptMassUpdateField
            field="adoptComparison"
            comparisonTitle={
              (
                meta.statisticsData as
                  | AddressCampaignValueStatistics
                  | undefined
              )?.comparisonCampaignTitle
            }
            setParam={setParam}
          />
        ),
      }}
      selectedEntries={(count) => (
        <SelectedEntriesPanel
          endpoint={massUpdate.endpoint}
          metadata={ADDRESS_CAMPAIGN_VALUE_PAGE.metadata}
          columns={ADDRESS_CAMPAIGN_VALUE_PAGE.columns}
          selectionKey={String(count)}
          count={count}
        />
      )}
    />
  );
}
