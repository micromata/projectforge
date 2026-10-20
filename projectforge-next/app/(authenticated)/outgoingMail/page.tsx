"use client";

import { EntityListPage } from "@/components/shared/list/entity-list-page";
import { POSTAUSGANG_PAGE } from "@/components/features/post/postausgang.page";

export default function PostausgangListPage() {
  return <EntityListPage page={POSTAUSGANG_PAGE} />;
}
