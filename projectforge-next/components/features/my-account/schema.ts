import { z } from "zod";
import { PF_USER_METADATA } from "@/lib/metadata/pf-user.generated";
import type { EntityMetadata } from "@/lib/metadata/types";
import { REQUIRED } from "@/lib/validation/markers";
import { fromMetadata } from "@/lib/validation/from-metadata";
import type { MyAccountData } from "./types";

/**
 * PFUserDO's metadata, but with a mandatory time zone: the entity falls back to the installation's one, while
 * this page always holds an explicit zone and `MyAccountRest.save` refuses an empty one.
 */
export const MY_ACCOUNT_METADATA = {
  ...PF_USER_METADATA,
  fields: {
    ...PF_USER_METADATA.fields,
    timeZone: { ...PF_USER_METADATA.fields.timeZone, required: true },
  },
} as const satisfies EntityMetadata;

/**
 * The editable fields of the my-account page, with the rules of PFUserDO (`lib/metadata/pf-user.generated.ts`):
 * a too long key is caught here before the server answers it with 406 (`MyAccountRest.save`).
 */
const m = fromMetadata(MY_ACCOUNT_METADATA);

export const myAccountSchema = z.object({
  firstname: m.nullableString("firstname"),
  lastname: m.nullableString("lastname"),
  // Choices from the server's lists (MyAccountRest.Options), the time zone an id of its suggestions.
  locale: z.string().nullable(),
  dateFormat: m.nullableString("dateFormat"),
  excelDateFormat: m.nullableString("excelDateFormat"),
  timeNotation: z.string().nullable(),
  timeZone: z
    .string()
    .nullable()
    .refine((v) => !!v?.trim(), REQUIRED),
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
