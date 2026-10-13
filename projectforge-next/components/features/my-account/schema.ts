import { z } from "zod";
import { PF_USER_METADATA } from "@/lib/metadata/pf-user.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { MyAccountData } from "./types";

/**
 * The editable fields of the my-account page, with the rules of PFUserDO (`lib/metadata/pf-user.generated.ts`):
 * a too long key is caught here before the server answers it with 406 (`MyAccountRest.save`).
 */
const m = fromMetadata(PF_USER_METADATA);

export const myAccountSchema = z.object({
  firstname: m.nullableString("firstname"),
  lastname: m.nullableString("lastname"),
  // Choices from the server's lists (MyAccountRest.Options), the time zone an id of its suggestions.
  locale: z.string().nullable(),
  dateFormat: m.nullableString("dateFormat"),
  excelDateFormat: m.nullableString("excelDateFormat"),
  timeNotation: z.string().nullable(),
  timeZone: z.string().nullable(),
  personalPhoneIdentifiers: m.nullableString("personalPhoneIdentifiers"),
  gpgPublicKey: m.nullableString("gpgPublicKey"),
  sshPublicKey: m.nullableString("sshPublicKey"),
});

export type MyAccountValues = z.infer<typeof myAccountSchema>;

export const MY_ACCOUNT_FIELDS = Object.keys(
  myAccountSchema.shape
) as readonly (keyof MyAccountValues)[];

export const EMPTY_VALUES: MyAccountValues = {
  firstname: null,
  lastname: null,
  locale: null,
  dateFormat: null,
  excelDateFormat: null,
  timeNotation: null,
  timeZone: null,
  personalPhoneIdentifiers: null,
  gpgPublicKey: null,
  sshPublicKey: null,
};

/** The form's values of the loaded data; a field Spring omitted becomes null. */
export function toFormValues(data: MyAccountData): MyAccountValues {
  return {
    firstname: data.firstname ?? null,
    lastname: data.lastname ?? null,
    locale: data.locale ?? null,
    dateFormat: data.dateFormat ?? null,
    excelDateFormat: data.excelDateFormat ?? null,
    timeNotation: data.timeNotation ?? null,
    timeZone: data.timeZone ?? null,
    personalPhoneIdentifiers: data.personalPhoneIdentifiers ?? null,
    gpgPublicKey: data.gpgPublicKey ?? null,
    sshPublicKey: data.sshPublicKey ?? null,
  };
}
