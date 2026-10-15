import type { LicenseValues } from "./license-schema";
import type { LicenseDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every
 * value is normalised here, so no field ever holds `undefined` — which a controlled input would read as
 * "uncontrolled" and the schema as a missing value.
 */
export function toFormValues(license: LicenseDetail): LicenseValues {
  return {
    id: license.id ?? null,
    organization: license.organization ?? null,
    product: license.product ?? "",
    version: license.version ?? "",
    updateFromVersion: license.updateFromVersion ?? null,
    device: license.device ?? null,
    numberOfLicenses: license.numberOfLicenses ?? null,
    owners: license.owners ?? [],
    validSince: license.validSince ?? null,
    validUntil: license.validUntil ?? null,
    licenseHolder: license.licenseHolder ?? null,
    key: license.key ?? null,
    comment: license.comment ?? null,
    status: license.status ?? null,
    created: license.created ?? null,
  };
}

/**
 * Blank form for a license that doesn't exist yet. The number of licenses is preset to one by the
 * backend (`LicenseEntityRest.newBaseDO`), whose `/rs/license/newEntry` answer the form is reset onto.
 */
export function emptyLicenseValues(): LicenseValues {
  return toFormValues({ id: null });
}
