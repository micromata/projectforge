"use client";

import { useTranslations } from "next-intl";
import { CopyableValue } from "@/components/shared/copyable-value";
import { SectionCard } from "@/components/shared/section-card";
import type { DataTransferView } from "@/lib/rs/datatransfer";
import { DataTransferInfoItem as Item } from "./datatransfer-info-item";
import { DataTransferObserveCheckbox } from "./datatransfer-observe-checkbox";

/**
 * What a reader of an area should know beside its files, as the legacy view listed it: who is notified
 * (and whether oneself is), the links to pass on, how long files are kept and how much fits, who has
 * access. The external link and password are the backend's to send: only to who may administer the area
 * (`DataTransferFilesRest`), and only while external access is on.
 *
 * A personal box shows neither observers nor access lists: it is one user's, and its owner is notified
 * anyway.
 */
export function DataTransferFilesInfo({ view }: { view: DataTransferView }) {
  const t = useTranslations();
  const area = view.area;
  const id = area.id!;
  const personal = area.personalBox === true;
  const yesNo = (value?: boolean | null) => (value ? t("yes") : t("no"));
  const internalLabel = t("plugins.datatransfer.internal.link");
  const externalLabel = t("plugins.datatransfer.external.link._");
  const passwordLabel = t("plugins.datatransfer.external.password._");

  return (
    <SectionCard>
      <dl className="grid gap-4 sm:grid-cols-3">
        {!personal && (
          <>
            <Item
              label={t("plugins.datatransfer.observers._")}
              hint={t("plugins.datatransfer.observers.info")}
              className="sm:col-span-2"
            >
              {area.observersAsString ?? "–"}
            </Item>
            <div className="flex items-end">
              <DataTransferObserveCheckbox
                id={id}
                checked={area.userWantsToObserve === true}
              />
            </div>
          </>
        )}
        <Item label={internalLabel} className="sm:col-span-2">
          {area.internalLink && (
            <CopyableValue value={area.internalLink} label={internalLabel} />
          )}
        </Item>
        <Item
          label={t("plugins.datatransfer.expiryDays._")}
          hint={t("plugins.datatransfer.expiryDays.info")}
        >
          {area.expiryDays}
        </Item>
        {view.editAccess && area.externalAccessEnabled && (
          <>
            <Item label={externalLabel} className="sm:col-span-2">
              <CopyableValue value={area.externalLink} label={externalLabel} />
            </Item>
            <Item label={passwordLabel}>
              {area.externalPassword && (
                <CopyableValue
                  value={area.externalPassword}
                  label={passwordLabel}
                  masked
                />
              )}
            </Item>
          </>
        )}
        <Item
          label={t("plugins.datatransfer.admins._")}
          className="sm:col-span-2"
        >
          {area.adminsAsString}
        </Item>
        <Item
          label={t("plugins.datatransfer.maxUploadSize._")}
          hint={t("plugins.datatransfer.maxUploadSize.info")}
        >
          {area.capacity?.maxUploadSizeFormatted}
        </Item>
        <Item label={t("plugins.datatransfer.capacity._")}>
          {area.capacity?.capacityAsMessage}
        </Item>
        {!personal && (
          <>
            <Item
              label={t("plugins.datatransfer.accessUsers._")}
              className="sm:col-span-3"
            >
              {area.accessUsersAsString}
            </Item>
            <Item
              label={t("plugins.datatransfer.accessGroups._")}
              className="sm:col-span-3"
            >
              {area.accessGroupsAsString}
            </Item>
            {area.externalAccessEnabled && (
              <>
                <Item
                  label={t("plugins.datatransfer.external.download.enabled._")}
                  hint={t(
                    "plugins.datatransfer.external.download.enabled.info"
                  )}
                >
                  {yesNo(area.externalDownloadEnabled)}
                </Item>
                <Item
                  label={t("plugins.datatransfer.external.upload.enabled._")}
                  hint={t("plugins.datatransfer.external.upload.enabled.info")}
                >
                  {yesNo(area.externalUploadEnabled)}
                </Item>
              </>
            )}
          </>
        )}
        <Item label={t("description")} className="sm:col-span-3">
          {area.description}
        </Item>
      </dl>
    </SectionCard>
  );
}
