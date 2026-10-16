"use client";

import { leafKeyOf } from "@/lib/leaf-key";
import { useTranslations } from "next-intl";
import { GearMenuItem } from "@/components/data-table";
import { useExportDownload } from "@/hooks/use-export-download";
import { downloadAppleScript } from "@/lib/rs/address";

/**
 * The address list's own entry of the gear menu (see PageDef.gearMenuActions): the AppleScript that removes the
 * notes of the addresses exported from ProjectForge from the Apple address book, as the former list offered it.
 */
export function AddressGearMenuActions() {
  const t = useTranslations();
  const script = useExportDownload(downloadAppleScript);
  return (
    <GearMenuItem
      label={t(leafKeyOf("address.book.export.appleScript4Notes", t.has))}
      description={t("address.book.export.appleScript4Notes.tooltip")}
      disabled={script.isPending}
      onSelect={() => script.mutate()}
    />
  );
}
