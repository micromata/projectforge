"use client";

import { useListMeta } from "@/hooks/use-list-meta";
import type { ADDRESS_METADATA } from "@/lib/metadata/address.generated";
import type { ColumnDeclaration } from "@/lib/page-def/types";
import { addressImageUrl } from "@/lib/rs/address";
import { AddressFavoriteToggle } from "./address-favorite-toggle";
import { AddressPhoneLinks } from "./address-phone-links";
import type { AddressListRow } from "./types";

/**
 * The columns of the address list, following the former server side layout (`AddressPagesRest`): the favorite
 * star and the names pinned left, the image as a thumbnail, the phone numbers as links that dial them.
 */
export const ADDRESS_COLUMNS: ColumnDeclaration<
  AddressListRow,
  typeof ADDRESS_METADATA
>[] = [
  {
    id: "isFavoriteCard",
    labelKey: "address.filter.myFavorites",
    headerLabelKey: "address.columnHead.myFavorites",
    accessor: (row) => row.isFavoriteCard === true,
    size: 48,
    sortable: false,
    filterKind: null,
    pinned: "left",
    cell: ({ row }) => (
      <AddressFavoriteToggle
        addressId={row.original.id}
        favorite={row.original.isFavoriteCard === true}
      />
    ),
  },
  { name: "name", size: 160, pinned: "left" },
  { name: "firstName", size: 140, pinned: "left" },
  { name: "lastUpdate", size: 110 },
  { name: "addressStatus", size: 120 },
  { name: "contactStatus", size: 120 },
  {
    id: "image",
    labelKey: "address.image",
    accessor: (row) => row.hasImage === true,
    size: 72,
    sortable: false,
    filterKind: null,
    cell: ({ row }) => <AddressThumbnail row={row.original} />,
  },
  { name: "organization", size: 200, wrap: true },
  { name: "email", size: 200 },
  {
    id: "phoneNumbers",
    labelKey: "address.phoneNumbers",
    accessor: (row) =>
      [
        row.businessPhone,
        row.mobilePhone,
        row.privatePhone,
        row.privateMobilePhone,
      ]
        .filter(Boolean)
        .join(", "),
    size: 200,
    sortable: false,
    wrap: true,
    cell: ({ row }) => <PhoneNumbersCell row={row.original} />,
  },
  {
    // The address books, joined. No sorting: the backend orders by entity property, and a collection is none.
    id: "addressbookList",
    labelKey: "address.addressbooks",
    accessor: (row) =>
      row.addressbookList
        ?.map((book) => book.displayName ?? book.title)
        .join(", "),
    size: 200,
    sortable: false,
    wrap: true,
  },
];

function AddressThumbnail({ row }: { row: AddressListRow }) {
  if (!row.hasImage) return null;
  return (
    // A plain img: the preview is a small, already shrunk thumbnail served by the backend
    // (`AddressImageServicesRest.getImagePreview`), nothing next/image could optimize under the static export.
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={addressImageUrl(row.id, true)}
      alt=""
      loading="lazy"
      className="max-h-12 max-w-16 rounded object-contain"
    />
  );
}

/** The flags come with the list's metadata (`AddressEntityRest.addVariablesForListPage`), read once for all rows. */
function PhoneNumbersCell({ row }: { row: AddressListRow }) {
  const variables = useListMeta("address").data?.variables;
  return (
    <AddressPhoneLinks
      address={row}
      phoneCallEnabled={variables?.phoneCallEnabled === true}
      smsEnabled={variables?.smsEnabled === true}
      callerPage="addressList"
    />
  );
}
