import type { DataTransferValues } from "./datatransfer-schema";
import type { DataTransferAreaDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(area: DataTransferAreaDetail): DataTransferValues {
  return {
    id: area.id ?? null,
    areaName: area.areaName ?? "",
    description: area.description ?? null,
    expiryDays: area.expiryDays ?? null,
    maxUploadSizeKB: area.maxUploadSizeKB ?? null,
    admins: area.admins ?? [],
    observers: area.observers ?? [],
    accessUsers: area.accessUsers ?? [],
    accessGroups: area.accessGroups ?? [],
    externalDownloadEnabled: area.externalDownloadEnabled ?? false,
    externalUploadEnabled: area.externalUploadEnabled ?? false,
    externalAccessToken: area.externalAccessToken ?? null,
    externalPassword: area.externalPassword ?? null,
    personalBox: area.personalBox ?? null,
    created: area.created ?? null,
  };
}

/**
 * Blank form for an area that doesn't exist yet. The backend presets the logged-in user as admin, the
 * expiry days and the upload size (`DataTransferAreaDao.createInitializedFile`), and its
 * `/rs/datatransfer/newEntry` answer is what the form is reset onto.
 */
export function emptyDataTransferValues(): DataTransferValues {
  return toFormValues({ id: null });
}
