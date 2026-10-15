"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { USER_PAGE } from "@/components/features/user/user.page";

export default function UserListPage() {
  return <EntityListPage page={USER_PAGE} />;
}
