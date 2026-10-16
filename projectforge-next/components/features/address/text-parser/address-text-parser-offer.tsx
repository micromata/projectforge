"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { FormAlert } from "@/components/shared/form-alert";
import { AddressFieldSelector } from "./address-field-selector";
import type { useAddressTextParser } from "./use-address-text-parser";

type Parser = ReturnType<typeof useAddressTextParser>;

/**
 * What the text parser found: the fields to tick, the image of a vCard, and the buttons to apply them to the form
 * or to drop the offer.
 */
export function AddressTextParserOffer({
  parser,
  onDiscard,
}: {
  parser: Parser;
  onDiscard: () => void;
}) {
  const t = useTranslations();
  const offered = parser.offered ?? {};
  const empty = Object.keys(offered).length === 0 && !parser.image;
  return (
    <div className="flex flex-col gap-3">
      {parser.warnings.length > 0 && (
        <FormAlert tone="info">
          <ul className="list-disc pl-4">
            {parser.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </FormAlert>
      )}
      {empty ? (
        <FormAlert tone="info">
          {t("address.parseText.info.noChanges")}
        </FormAlert>
      ) : (
        <>
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-sm font-semibold">
              {t("address.parseText.fieldsParsed")}
            </span>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={parser.selectAll}
            >
              {t("common.import.action.selectAll")}
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={parser.selectNone}
            >
              {t("common.import.action.deselectAll")}
            </Button>
          </div>
          <AddressFieldSelector
            offered={offered}
            selection={parser.selection}
            onToggle={parser.toggle}
            fieldMappings={parser.fieldMappings}
            onFieldMapping={parser.setFieldMapping}
            blockMappings={parser.blockMappings}
            onBlockMapping={parser.setBlockMapping}
          />
          {parser.image && (
            <div className="flex items-center gap-3">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={parser.image.previewUrl}
                alt={t("address.image")}
                className="max-h-24 max-w-24 rounded-md border object-contain"
              />
              {parser.image.tooLarge ? (
                <span className="text-sm text-destructive">
                  {t("address.book.vCardsImport.imageTooLarge")}
                </span>
              ) : (
                <label className="flex items-center gap-2 text-sm">
                  <Checkbox
                    checked={parser.image.apply}
                    onCheckedChange={(value) =>
                      parser.setApplyImage(value === true)
                    }
                  />
                  {t("address.book.vCardsImport.importImage")}
                </label>
              )}
            </div>
          )}
        </>
      )}
      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={onDiscard}>
          {t("cancel")}
        </Button>
        {!empty && (
          <Button
            type="button"
            disabled={parser.busy}
            onClick={() => void parser.apply()}
          >
            {t("apply")}
          </Button>
        )}
      </div>
    </div>
  );
}
