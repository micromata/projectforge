"use client";

import { leafKeyOf } from "@/lib/leaf-key";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ContactBookIcon,
  FileImportIcon,
  QrCodeIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  ExcelExportMenuItem,
  ExportMenu,
  ExportMenuItem,
} from "@/components/shared/export-menu";
import { useExportDownload } from "@/hooks/use-export-download";
import { useListMeta } from "@/hooks/use-list-meta";
import { navigateInGesture } from "@/lib/navigate-in-gesture";
import {
  downloadFavoritesExcel,
  downloadFavoritesVCards,
} from "@/lib/rs/address";
import { downloadListExcel } from "@/lib/rs/list-export";
import type { MagicFilter } from "@/lib/rs/types";
import { CardDavInfoDialog } from "./carddav-info-dialog";

/**
 * The toolbar of the address list, as the former list offered it: the VCF import, and the exports — the
 * filtered list as Excel, the favorites as vCards (to import into an address book app) and as Excel, and the
 * CardDAV access data where the CardDAV server is enabled (`variables.carddavEnabled`). Besides, the own digital
 * business cards (also in the user menu).
 */
export function AddressListActions({ filter }: { filter: MagicFilter }) {
  const t = useTranslations();
  const router = useRouter();
  const [cardDav, setCardDav] = useState(false);
  const carddavEnabled =
    useListMeta("address").data?.variables?.carddavEnabled === true;
  const excel = useExportDownload(() => downloadListExcel("address", filter));
  const vCards = useExportDownload(downloadFavoritesVCards);
  const favoritesExcel = useExportDownload(downloadFavoritesExcel);

  return (
    <>
      <Button
        type="button"
        variant="outline"
        onClick={() => navigateInGesture(router, "/address/import")}
      >
        <HugeiconsIcon icon={FileImportIcon} />
        {t("import._")}
      </Button>
      <Button
        type="button"
        variant="outline"
        onClick={() => navigateInGesture(router, "/myBusinessCards")}
      >
        <HugeiconsIcon icon={QrCodeIcon} />
        {t("menu.myBusinessCards")}
      </Button>
      <ExportMenu
        isPending={
          excel.isPending || vCards.isPending || favoritesExcel.isPending
        }
      >
        <ExcelExportMenuItem onSelect={() => excel.mutate()} />
        <ExportMenuItem
          label={t(leafKeyOf("address.book.vCardExport", t.has))}
          description={t("address.book.vCardExport.tooltip.content")}
          onSelect={() => vCards.mutate()}
        />
        <ExportMenuItem
          label={t(leafKeyOf("address.book.export", t.has))}
          description={t("address.book.export.tooltip")}
          onSelect={() => favoritesExcel.mutate()}
        />
        {carddavEnabled && (
          <ExportMenuItem
            icon={ContactBookIcon}
            label={t("address.cardDAV.infopage.title")}
            description={t("address.favorites.info")}
            onSelect={() => setCardDav(true)}
          />
        )}
      </ExportMenu>
      {cardDav && <CardDavInfoDialog onClose={() => setCardDav(false)} />}
    </>
  );
}
