/**
 * The send dialogs of an address (direct call, text message) keep their state in the query of the page they open
 * over (see useUrlDialog), so a reload or a copied url shows them again and Back closes them (the React app
 * routed them).
 *
 * [from] is the place that opened it (`callerPage` of AddressPhoneLinks): the numbers of one address are shown
 * in the list row and, over it, in the view dialog — only the one that was clicked shows the dialog.
 */
export type AddressDialog = { addressId: number; from: string } & (
  { kind: "call"; number: string } | { kind: "sms"; phoneType: string }
);

/** The params a dialog owns, for `useUrlDialog`. */
export const ADDRESS_DIALOG_KEYS = [
  "dialog",
  "dialogAddress",
  "dialogFrom",
  "number",
  "phoneType",
] as const;

/** The dialog the query asks for, or null (also for an incomplete one). */
export function parseAddressDialog(
  params: URLSearchParams
): AddressDialog | null {
  const addressId = Number(params.get("dialogAddress"));
  const from = params.get("dialogFrom");
  if (!Number.isInteger(addressId) || addressId <= 0 || !from) return null;
  const kind = params.get("dialog");
  const number = params.get("number");
  const phoneType = params.get("phoneType");
  if (kind === "call" && number) return { kind, addressId, from, number };
  if (kind === "sms" && phoneType) return { kind, addressId, from, phoneType };
  return null;
}

/** The params that open [dialog]. */
export function addressDialogParams(
  dialog: AddressDialog
): Record<string, string> {
  return {
    dialog: dialog.kind,
    dialogAddress: String(dialog.addressId),
    dialogFrom: dialog.from,
    ...(dialog.kind === "call"
      ? { number: dialog.number }
      : { phoneType: dialog.phoneType }),
  };
}
