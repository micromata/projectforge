"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import {
  applyParsedAddressData,
  applyVcfImage,
  parseAddressText,
  parseVcfFile,
  uploadedAddressImageUrl,
  VCF_IMAGE_PREVIEW_URL,
} from "@/lib/rs/address";
import type { AddressImportEntryComparison } from "@/lib/rs/address-import";
import { toast } from "@/lib/toast";
import type { AddressValues } from "../address-schema";
import {
  changedFields,
  fieldsFromComparison,
  fieldsToApply,
  initialSelection,
  selectAll,
  type AddressBlock,
  type OfferedFields,
} from "./text-parser-model";

/** The image of a dropped vCard (or of an import entry), staged in the session. */
export interface OfferedImage {
  previewUrl: string;
  tooLarge: boolean;
  apply: boolean;
}

/**
 * The state of the text parser: the fields offered by a pasted text, a dropped vCard or the import entry the form
 * was opened for, which of them are ticked and where they land, and applying them to the form.
 *
 * Applying goes through the backend (`address/applyParsedData`), which converts what can't be posted as text (the
 * birthday, the form of address, the language); only the ticked fields are taken over from its answer.
 */
export function useAddressTextParser(
  importEntry?: AddressImportEntryComparison
) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const values = () => form.state.values as AddressValues;
  const [offered, setOffered] = useState<OfferedFields | null>(null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [selection, setSelection] = useState<Record<string, boolean>>({});
  const [fieldMappings, setFieldMappings] = useState<Record<string, string>>(
    {}
  );
  const [blockMappings, setBlockMappings] = useState<
    Partial<Record<AddressBlock, AddressBlock>>
  >({});
  const [image, setImage] = useState<OfferedImage | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const offer = (fields: OfferedFields, offeredImage: OfferedImage | null) => {
    setOffered(fields);
    setSelection(initialSelection(fields, values()));
    setFieldMappings({});
    setBlockMappings({});
    setImage(offeredImage);
    setError(null);
  };

  // The comparison of the import entry is offered as soon as it is there: comparing is what the form is open for.
  // Adjusted while rendering rather than in an effect (React's pattern for state derived from a prop), once per
  // entry: the values are read when it arrives, later edits of the form don't re-offer it.
  const [offeredEntry, setOfferedEntry] =
    useState<AddressImportEntryComparison>();
  if (importEntry && importEntry !== offeredEntry) {
    setOfferedEntry(importEntry);
    offer(
      fieldsFromComparison(importEntry.vcfComparisonData, values()),
      stagedImage(importEntry.hasImage, importEntry.imageTooLarge)
    );
    setWarnings([]);
  }

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    try {
      await action();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  };

  const parseText = (text: string) =>
    run(async () => {
      if (!text.trim()) {
        setError(t("address.parseText.error.emptyInput"));
        return;
      }
      const parsed = await parseAddressText(text);
      offer(changedFields(parsed?.fields ?? {}, values()), null);
      setWarnings(parsed?.warnings ?? []);
    });

  const parseVcf = (file: File) =>
    run(async () => {
      if (!file.name.toLowerCase().endsWith(".vcf")) {
        setError(t("address.book.vCardsImport.wrongFileType"));
        return;
      }
      const result = await parseVcfFile(file, values().id);
      if (result.error || !result.parsedData) {
        setError(result.error ?? t("address.book.vCardsImport.error.parsing"));
        return;
      }
      offer(
        result.parsedData.fields,
        stagedImage(result.hasImage === true, result.imageTooLarge === true)
      );
      setWarnings(result.parsedData.warnings ?? []);
    });

  const apply = () =>
    run(async () => {
      if (!offered) return;
      const fields = fieldsToApply(
        offered,
        selection,
        fieldMappings,
        blockMappings
      );
      if (Object.keys(fields).length > 0) {
        const applied = await applyParsedAddressData(values(), fields);
        const data = (applied ?? {}) as Record<string, unknown>;
        for (const field of Object.keys(fields)) {
          const value = data[field];
          form.setFieldValue(
            field,
            value === "" || value === undefined ? null : value
          );
        }
      }
      if (image?.apply && !image.tooLarge) {
        await applyVcfImage();
        form.setFieldValue("imageDataUrl", uploadedAddressImageUrl(Date.now()));
        form.setFieldValue("imageDeleted", false);
      }
      toast.success(t("address.parseText.applied"));
      setOffered(null);
      setImage(null);
      setWarnings([]);
    });

  return {
    offered,
    warnings,
    selection,
    toggle: (field: string) =>
      setSelection((current) => ({ ...current, [field]: !current[field] })),
    selectAll: () => {
      if (offered)
        setSelection((current) => selectAll(offered, values(), current));
      setImage(
        (current) => current && { ...current, apply: !current.tooLarge }
      );
    },
    selectNone: () => {
      setSelection({});
      setImage((current) => current && { ...current, apply: false });
    },
    fieldMappings,
    setFieldMapping: (field: string, target: string) =>
      setFieldMappings((current) => ({ ...current, [field]: target })),
    blockMappings,
    setBlockMapping: (block: AddressBlock, target: AddressBlock) =>
      setBlockMappings((current) => ({ ...current, [block]: target })),
    image,
    setApplyImage: (apply: boolean) =>
      setImage((current) => current && { ...current, apply }),
    busy,
    error,
    discard: () => {
      setOffered(null);
      setImage(null);
      setWarnings([]);
      setError(null);
    },
    parseText,
    parseVcf,
    apply,
  };
}

function stagedImage(
  hasImage: boolean,
  tooLarge: boolean
): OfferedImage | null {
  if (!hasImage) return null;
  return {
    previewUrl: `${VCF_IMAGE_PREVIEW_URL}?v=${Date.now()}`,
    tooLarge,
    apply: !tooLarge,
  };
}
