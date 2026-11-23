"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Pdf01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Spinner } from "@/components/shared/spinner";
import { useExportDownload } from "@/hooks/use-export-download";
import { downloadAccountDataSheet } from "@/lib/rs/account";

/**
 * "Customer data sheet": the invoice data of the account as a PDF to send to the customer for checking —
 * what is known is filled in, the rest is left blank (`KontoEntityRest.getCustomerDataSheet`).
 *
 * Disabled for an account that isn't stored yet, because the sheet is made from the stored account.
 */
export function CustomerDataSheetButton({ id }: { id: number | null }) {
  const t = useTranslations();
  // A wrapper, not the function itself: the mutation hands its function a second argument (its context),
  // which would land in the function's `signal` parameter and make fetch fail.
  const download = useExportDownload((accountId: number) =>
    downloadAccountDataSheet(accountId)
  );
  return (
    <HintTooltip text={t("fibu.konto.customerDataSheet.tooltip")}>
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
        disabled={id == null || download.isPending}
        onClick={() => id != null && download.mutate(id)}
      >
        {download.isPending ? (
          <Spinner className="h-3.5 w-3.5 border-2" />
        ) : (
          <HugeiconsIcon icon={Pdf01Icon} size={14} aria-hidden />
        )}
        {/* "._": the key has a child (the tooltip), so its own text lives in the leaf `_`. */}
        {t("fibu.konto.customerDataSheet._")}
      </Button>
    </HintTooltip>
  );
}
