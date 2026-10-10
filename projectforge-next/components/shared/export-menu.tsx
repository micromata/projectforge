"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import type { IconSvgElement } from "@hugeicons/react";
import { Download04Icon } from "@hugeicons/core-free-icons";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { DescribedMenuItem } from "@/components/shared/described-menu-item";
import { ExportButton } from "@/components/shared/export-button";
import { useExportDownload } from "@/hooks/use-export-download";

/**
 * The exports of a list toolbar (see PageDef.listActions) behind one "Export" button, each entry with its
 * explanation in the entry itself (see [DescribedMenuItem]) rather than in a tooltip.
 *
 * The downloads are the caller's ([useExportDownload]), not the entries': the menu unmounts its entries as
 * soon as it closes — which it does on the very click that starts the download — so a mutation held by an
 * entry would lose its pending state at once. The caller hands that state in as `isPending`, and the
 * trigger shows the spinner of [ExportButton] while any export runs.
 *
 * Always the last of a list's actions, so it sits right beside the gear menu on every list: imports,
 * links and other buttons of the page go before it.
 */
export function ExportMenu({
  isPending = false,
  children,
}: {
  isPending?: boolean;
  /** [ExportMenuItem]s. */
  children: ReactNode;
}) {
  const t = useTranslations();
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <ExportButton label={t("export")} isPending={isPending} />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-72">
        {children}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/** One export of an [ExportMenu]: the download icon unless the format has one of its own. */
export function ExportMenuItem({
  icon = Download04Icon,
  ...props
}: {
  label: string;
  description: string;
  icon?: IconSvgElement;
  disabled?: boolean;
  onSelect: () => void;
}) {
  return <DescribedMenuItem icon={icon} {...props} />;
}

/**
 * The plain Excel export of a list, as an entry of its [ExportMenu]: the label and explanation every list
 * shares, started by the caller (see [ExportMenu]).
 */
export function ExcelExportMenuItem({ onSelect }: { onSelect: () => void }) {
  const t = useTranslations();
  return (
    <ExportMenuItem
      label={t("exportAsXls")}
      description={t("tooltip.export.excel")}
      onSelect={onSelect}
    />
  );
}

/** The export menu of a list whose only export is the plain Excel export of the filtered rows. */
export function ExcelExportMenu({
  download,
}: {
  download: () => Promise<void>;
}) {
  const excel = useExportDownload(download);
  return (
    <ExportMenu isPending={excel.isPending}>
      <ExcelExportMenuItem onSelect={() => excel.mutate()} />
    </ExportMenu>
  );
}
