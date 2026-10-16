"use client";

import type { ComponentType } from "react";
import { DeclaredFormField } from "@/components/shared/edit/declared-form-field";
import { ADDRESS_FORM_METADATA, type AddressValues } from "./address-schema";
import {
  AddressText2Field,
  AddressTextField,
  PostalAddressText2Field,
  PostalAddressTextField,
  PrivateAddressText2Field,
  PrivateAddressTextField,
} from "./address-suggest-fields";

export type AddressKind = "business" | "postal" | "private";
type Metadata = typeof ADDRESS_FORM_METADATA;
type AddressField = keyof AddressValues & keyof Metadata["fields"];

export interface AddressKindDef {
  kind: AddressKind;
  headingKey: string;
  /** The two text lines (completed from other addresses) and the four fields of the place. */
  text: ComponentType<{ className?: string }>;
  text2: ComponentType<{ className?: string }>;
  place: [AddressField, AddressField, AddressField, AddressField];
}

export const ADDRESS_KINDS: AddressKindDef[] = [
  {
    kind: "business",
    headingKey: "address.heading.businessAddress",
    text: AddressTextField,
    text2: AddressText2Field,
    place: ["zipCode", "city", "country", "state"],
  },
  {
    kind: "postal",
    headingKey: "address.heading.postalAddress",
    text: PostalAddressTextField,
    text2: PostalAddressText2Field,
    place: ["postalZipCode", "postalCity", "postalCountry", "postalState"],
  },
  {
    kind: "private",
    headingKey: "address.heading.privateAddress",
    text: PrivateAddressTextField,
    text2: PrivateAddressText2Field,
    place: ["privateZipCode", "privateCity", "privateCountry", "privateState"],
  },
];

/** The six form fields of a kind; the text lines are named after their property like the suggest fields. */
export function fieldNamesOf(def: AddressKindDef): AddressField[] {
  const prefix = def.kind === "business" ? "" : def.kind;
  const text = (name: string) =>
    (prefix
      ? `${prefix}${name.charAt(0).toUpperCase()}${name.slice(1)}`
      : name) as AddressField;
  return [text("addressText"), text("addressText2"), ...def.place];
}

/**
 * The fields of one kind as they are written on an envelope: the two text lines one under the other, the zip code
 * beside the city, the state beside the country.
 */
export function AddressBlockFields({ def }: { def: AddressKindDef }) {
  const [zipCode, city, country, state] = def.place;
  const Text = def.text;
  const Text2 = def.text2;
  const field = (name: AddressField, className: string) => (
    <DeclaredFormField
      field={{ name }}
      metadata={ADDRESS_FORM_METADATA}
      className={className}
    />
  );
  return (
    <div className="flex flex-col gap-4">
      <Text />
      <Text2 />
      <div className="flex gap-4">
        {field(zipCode, "w-28 shrink-0")}
        {field(city, "min-w-0 flex-1")}
      </div>
      <div className="flex gap-4">
        {field(country, "min-w-0 flex-1")}
        {field(state, "min-w-0 flex-1")}
      </div>
    </div>
  );
}
