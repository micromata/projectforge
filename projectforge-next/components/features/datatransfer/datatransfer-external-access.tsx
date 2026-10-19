"use client";

import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { CheckboxField } from "@/components/shared/form/checkbox-field";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { FormAlert } from "@/components/shared/form-alert";
import { cn } from "@/lib/utils";
import type { DataTransferValues } from "./datatransfer-schema";
import { DataTransferExternalSecrets } from "./datatransfer-external-secrets";
import type { DataTransferAreaDetail } from "./types";
import { useDataTransferOptions } from "./use-datatransfer-options";
import { useExternalSecrets } from "./use-external-secrets";

/**
 * The external access of an area: the two switches, and — once either is on — the password and the link
 * external users open (see DataTransferExternalSecrets).
 *
 * Via the gateway (`externalAccessViaGateway`), the switches work as usual, and a note says that the link,
 * the external transfers and the files are on the gateway, to which the area is pushed.
 *
 * Where external access isn't allowed here (`externalAccessAllowed`), a note says so instead. A switch is
 * then shown only if the stored area has it on (created before), and only for switching it off: once off, it
 * can't be turned on again (the server refuses that too).
 */
export function DataTransferExternalAccess({
  className,
}: {
  className?: string;
}) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const data = useEntityData<DataTransferAreaDetail>();
  const options = useDataTransferOptions();
  const values = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => state.values as DataTransferValues
  );
  const enabled =
    values.externalDownloadEnabled || values.externalUploadEnabled;
  const allowed = options?.externalAccessAllowed ?? true;
  const showDownload = allowed || data?.externalDownloadEnabled === true;
  const showUpload = allowed || data?.externalUploadEnabled === true;
  useExternalSecrets(
    form,
    enabled,
    values.externalAccessToken,
    values.externalPassword
  );

  return (
    <div className={cn("flex flex-col gap-4", className)}>
      {options && !allowed && (
        <FormAlert tone="info">
          {t("plugins.datatransfer.gateway.externalAccess.edit")}
        </FormAlert>
      )}
      {options?.externalAccessViaGateway && (
        <FormAlert tone="info">
          {t("plugins.datatransfer.gateway.externalAccess.viaGateway", {
            arg0: options.gatewayUrl ?? "",
          })}
        </FormAlert>
      )}
      {(showDownload || showUpload) && (
        <div className="grid gap-4 sm:grid-cols-2">
          {showDownload && (
            <CheckboxField
              name="externalDownloadEnabled"
              label={t("plugins.datatransfer.external.download.enabled._")}
              hint={t("plugins.datatransfer.external.download.enabled.info")}
              disabled={!allowed && !values.externalDownloadEnabled}
            />
          )}
          {showUpload && (
            <CheckboxField
              name="externalUploadEnabled"
              label={t("plugins.datatransfer.external.upload.enabled._")}
              hint={t("plugins.datatransfer.external.upload.enabled.info")}
              disabled={!allowed && !values.externalUploadEnabled}
            />
          )}
        </div>
      )}
      {enabled && (
        <DataTransferExternalSecrets
          token={values.externalAccessToken}
          storedLink={data?.externalLink}
          storedToken={data?.externalAccessToken}
        />
      )}
    </div>
  );
}
