"use client";

import { useTranslations } from "next-intl";
import { EditPageTabs } from "@/components/shared/edit-page-tabs";
import { HistorySection } from "@/components/shared/history/history-section";
import { useEntityDetail, type EntityWithId } from "@/hooks/use-entity-detail";
import { useLegacyEditUrl } from "@/hooks/use-legacy-edit-url";
import type { ListRow } from "@/hooks/use-entity-list-page";
import type { EntityMetadata } from "@/lib/metadata/types";
import type { PageDef } from "@/lib/page-def/types";
import { EntityEditHeader } from "./entity-edit-header";
import { entityTabs, HISTORY_TAB_ID } from "./entity-tabs";

/**
 * The change history of one entry, rendered from the entity's declaration.
 *
 * A page of its own rather than a section of the form, so a long history is only built when it is
 * actually looked at — the server assembles it per request.
 *
 * The entry itself is read for the header; coming from the form it is in the cache already.
 */
export function EntityHistoryPage<
  Row extends ListRow,
  Values,
  Data extends EntityWithId,
  M extends EntityMetadata,
>({ page, id }: { page: PageDef<Row, Values, Data, M>; id: number }) {
  const t = useTranslations();
  const { data } = useEntityDetail<Data>(page.entity, id);
  const legacyUrl = useLegacyEditUrl(page.entity, id);

  const tabs = entityTabs({
    sections: page.edit.sections,
    t,
    id,
    route: page.route,
    history: page.edit.history,
    extraTabs: page.edit.extraTabs,
    onFormPage: false,
  });

  return (
    <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
      <div className="shrink-0">
        <EntityEditHeader
          listRoute={page.route}
          listLabel={t(page.titleKey)}
          title={data ? page.edit.title(data) : ""}
          legacyUrl={legacyUrl}
        />
      </div>
      <EditPageTabs tabs={tabs} activeId={HISTORY_TAB_ID} />
      <div className="flex-1 overflow-y-auto bg-muted/30 px-6 pb-6 pt-4">
        <HistorySection entity={page.entity} entityId={id} />
      </div>
    </div>
  );
}
