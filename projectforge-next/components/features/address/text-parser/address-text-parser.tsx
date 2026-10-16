"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, ArrowRight01Icon } from "@hugeicons/core-free-icons";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { FormAlert } from "@/components/shared/form-alert";
import { leafKeyOf } from "@/lib/leaf-key";
import { useFormReadOnly } from "@/components/shared/form/form-context";
import type { AddressImportEntryComparison } from "@/lib/rs/address-import";
import { AddressTextParserInput } from "./address-text-parser-input";
import { AddressTextParserOffer } from "./address-text-parser-offer";
import { useAddressTextParser } from "./use-address-text-parser";

/**
 * Takes data over into the form from a pasted text (an e-mail signature) or a dropped vCard — or, for an entry of
 * a VCF import, from its vCard, compared field by field with the stored address. Folded unless the form was opened
 * for an import entry: comparing is what it is open for then.
 */
export function AddressTextParser({
  importEntry,
}: {
  importEntry?: AddressImportEntryComparison;
}) {
  const t = useTranslations();
  const readOnly = useFormReadOnly();
  const parser = useAddressTextParser(importEntry);
  // Until the user folds or unfolds it, it is open exactly for an import entry.
  const [toggled, setToggled] = useState<boolean | null>(null);
  if (readOnly) return null;
  const expanded = toggled ?? importEntry !== undefined;

  return (
    <Collapsible
      open={expanded}
      onOpenChange={setToggled}
      className="rounded-md border border-border bg-card"
    >
      <CollapsibleTrigger className="flex w-full items-center gap-2 px-4 py-3 text-left text-sm font-semibold">
        <HugeiconsIcon
          icon={expanded ? ArrowDown01Icon : ArrowRight01Icon}
          size={16}
        />
        {/* Both a text and the parent of `.tooltip`, so the generator exports it as its leaf. */}
        {t(leafKeyOf("address.parseText.button", t.has))}
        <span className="text-xs font-normal text-muted-foreground">
          {t("address.parseText.button.tooltip")}
        </span>
      </CollapsibleTrigger>
      <CollapsibleContent className="flex flex-col gap-4 px-4 pb-4">
        {parser.offered === null ? (
          <AddressTextParserInput
            busy={parser.busy}
            onParseText={(text) => void parser.parseText(text)}
            onParseVcf={(file) => void parser.parseVcf(file)}
          />
        ) : (
          <AddressTextParserOffer parser={parser} onDiscard={parser.discard} />
        )}
        {parser.error && <FormAlert tone="error">{parser.error}</FormAlert>}
      </CollapsibleContent>
    </Collapsible>
  );
}
