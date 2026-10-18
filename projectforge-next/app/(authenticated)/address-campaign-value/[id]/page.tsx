import { AddressCampaignValueEditPageClient } from "./page-client";

export function generateStaticParams() {
  return [{ id: "new" }];
}

export default function AddressCampaignValueEditPage() {
  return <AddressCampaignValueEditPageClient />;
}
