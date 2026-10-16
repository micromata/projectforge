"use client";

import { useTranslations } from "next-intl";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ADDRESS_METADATA } from "@/lib/metadata/address.generated";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { AddressOfferRow } from "./address-offer-row";
import {
  ADDRESS_BLOCKS,
  blockOf,
  EMAIL_FIELDS,
  isEmailField,
  isPhoneField,
  PHONE_FIELDS,
  type AddressBlock,
  type OfferedFields,
} from "./text-parser-model";

interface Props {
  offered: OfferedFields;
  selection: Record<string, boolean>;
  onToggle: (field: string) => void;
  fieldMappings: Record<string, string>;
  onFieldMapping: (field: string, target: string) => void;
  blockMappings: Partial<Record<AddressBlock, AddressBlock>>;
  onBlockMapping: (block: AddressBlock, target: AddressBlock) => void;
}

/**
 * The offered fields of the text parser, each with a box to tick it and the value it would replace. A phone
 * number or an e-mail may be moved to another kind (a mobile number the text called "phone"), an address block
 * as a whole to another block (a business address that is the postal one).
 */
export function AddressFieldSelector({
  offered,
  selection,
  onToggle,
  fieldMappings,
  onFieldMapping,
  blockMappings,
  onBlockMapping,
}: Props) {
  const t = useTranslations();
  const labelOf = useOfferLabels();
  const entries = Object.entries(offered);
  const single = entries.filter(([field]) => blockOf(field) === null);

  return (
    <div className="flex flex-col gap-3">
      <ul className="flex flex-col gap-1.5">
        {single.map(([field, offer]) => {
          const kinds = isPhoneField(field)
            ? PHONE_FIELDS
            : isEmailField(field)
              ? EMAIL_FIELDS
              : null;
          return (
            <AddressOfferRow
              key={field}
              field={field}
              offer={offer}
              checked={selection[field] === true}
              onToggle={() => onToggle(field)}
              label={
                kinds ? (
                  <Select
                    value={fieldMappings[field] ?? field}
                    onValueChange={(target) => onFieldMapping(field, target)}
                  >
                    <SelectTrigger
                      size="sm"
                      aria-label={t("address.parseText.remapTo")}
                    >
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {kinds.map((kind) => (
                        <SelectItem key={kind} value={kind}>
                          {labelOf(kind)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                ) : (
                  labelOf(field)
                )
              }
            />
          );
        })}
      </ul>
      {ADDRESS_BLOCKS.map((block) => {
        const fields = entries.filter(([field]) => blockOf(field) === block);
        if (fields.length === 0) return null;
        return (
          <fieldset key={block} className="rounded-md border p-3">
            <legend className="flex items-center gap-2 px-1 text-xs font-semibold">
              {t("address.parseText.addressBlock")}
              <Select
                value={blockMappings[block] ?? block}
                onValueChange={(target) =>
                  onBlockMapping(block, target as AddressBlock)
                }
              >
                <SelectTrigger
                  size="sm"
                  aria-label={t("address.parseText.remapTo")}
                >
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ADDRESS_BLOCKS.map((target) => (
                    <SelectItem key={target} value={target}>
                      <BlockLabel block={target} />
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </legend>
            <ul className="flex flex-col gap-1.5">
              {fields.map(([field, offer]) => (
                <AddressOfferRow
                  key={field}
                  field={field}
                  offer={offer}
                  checked={selection[field] === true}
                  onToggle={() => onToggle(field)}
                  label={labelOf(field)}
                />
              ))}
            </ul>
          </fieldset>
        );
      })}
    </div>
  );
}

/** Literal keys, so the i18n generator finds them. */
function BlockLabel({ block }: { block: AddressBlock }) {
  const t = useTranslations();
  if (block === "postal") return t("address.parseText.addressType.postal");
  if (block === "private") return t("address.parseText.addressType.private");
  return t("address.parseText.addressType.business");
}

/**
 * The label of an offered field: the kind of a phone number or e-mail spelled out (the entity calls each of them
 * just "phone"), every other field as the form labels it.
 */
function useOfferLabels() {
  const t = useTranslations();
  const label = useFieldLabels(ADDRESS_METADATA);
  const kinds: Record<string, string> = {
    businessPhone: t("address.phoneType.business"),
    mobilePhone: t("address.phoneType.mobile"),
    fax: t("address.phoneType.fax"),
    privatePhone: t("address.phoneType.private"),
    privateMobilePhone: t("address.phoneType.privateMobile"),
    email: `${t("email")} (${t("address.business")})`,
    privateEmail: t("address.privateEmail"),
  };
  return (field: string) =>
    kinds[field] ??
    (field in ADDRESS_METADATA.fields
      ? label(field as keyof typeof ADDRESS_METADATA.fields)
      : field);
}
