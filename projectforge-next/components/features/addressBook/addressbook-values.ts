import type { AddressbookValues } from "./addressbook-schema";
import type { AddressbookDetail } from "./types";

/**
 * A field Spring left out of the JSON (`JsonInclude.Include.NON_NULL`) arrives as `undefined`; every value is
 * normalised here, so no field ever holds `undefined`.
 */
export function toFormValues(ab: AddressbookDetail): AddressbookValues {
  return {
    id: ab.id ?? null,
    title: ab.title ?? "",
    description: ab.description ?? null,
    owner: ab.owner ?? null,
    fullAccessUsers: ab.fullAccessUsers ?? [],
    fullAccessGroups: ab.fullAccessGroups ?? [],
    readonlyAccessUsers: ab.readonlyAccessUsers ?? [],
    readonlyAccessGroups: ab.readonlyAccessGroups ?? [],
  };
}

/**
 * Blank form for an address book that doesn't exist yet. The backend presets the logged-in user as owner
 * (`AddressbookEntityRest.newBaseDTO`), and its `/rs/addressBook/newEntry` answer is what the form is reset onto.
 */
export function emptyAddressbookValues(): AddressbookValues {
  return toFormValues({ id: null });
}
