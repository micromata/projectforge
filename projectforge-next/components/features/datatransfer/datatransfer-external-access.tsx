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
 * In gateway mode the external access is administered on the other server, which the note above says,
 * as the legacy form did; the switches stay, as they did there too.
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
  useExternalSecrets(
    form,
    enabled,
    values.externalAccessToken,
    values.externalPassword
  );

  return (
    <div className={cn("flex flex-col gap-4", className)}>
      {options?.gatewayPushEnabled && (
        <FormAlert tone="info">
          {t("plugins.datatransfer.gateway.externalAccess.edit", {
            arg0: options.gatewayHost,
          })}
        </FormAlert>
      )}
      <div className="grid gap-4 sm:grid-cols-2">
        <CheckboxField
          name="externalDownloadEnabled"
          label={t("plugins.datatransfer.external.download.enabled._")}
          hint={t("plugins.datatransfer.external.download.enabled.info")}
        />
        <CheckboxField
          name="externalUploadEnabled"
          label={t("plugins.datatransfer.external.upload.enabled._")}
          hint={t("plugins.datatransfer.external.upload.enabled.info")}
        />
      </div>
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
