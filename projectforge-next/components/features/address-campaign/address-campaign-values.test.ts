import { describe, expect, it } from "vitest";
import { addressCampaignSchema } from "./address-campaign-schema";
import {
  emptyAddressCampaignValues,
  toFormValues,
} from "./address-campaign-values";

describe("toFormValues", () => {
  // The form's values are posted as the DTO: without the id, every save of an existing campaign was
  // an insert, and setting a campaign inactive duplicated it.
  it("keeps the id of a saved campaign, so saving it updates it", () => {
    const values = toFormValues({
      id: 42,
      title: "Campaign",
      values: "Yes; No",
      active: false,
    });
    expect(values.id).toBe(42);
    expect(addressCampaignSchema.parse(values).id).toBe(42);
  });

  it("has no id for a new campaign", () => {
    expect(emptyAddressCampaignValues().id).toBeNull();
  });
});
