"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { FileValidationIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { resolveMenuUrl, toAbsoluteUrl } from "@/lib/menu-url";

/**
 * The e-invoice checker — the page that reads an uploaded XRechnung or ZUGFeRD file back and says what is
 * in it (`EInvoiceCheckerPageRest`).
 *
 * Reachable from the menu, and from here as well: it is the counterpart of the export, so the two places one
 * looks for it are the invoice one just exported and the list one exports from. Nothing but a link — the
 * checker takes a file, not an invoice, so there is no state to hand it.
 *
 * Not yet a page of this app (`MenuItemDefId.E_INVOICE_CHECKER` points into the legacy React app), so the
 * href goes through `resolveMenuUrl` like every menu entry: a full page load, and one line to change when the
 * page is migrated.
 *
 * In a tab of its own, because it is used *beside* the invoice: on the edit page leaving would drop unsaved
 * changes, and comparing the exported file with the form is the very thing one opens it for.
 */
export function EInvoiceCheckerButton() {
  const t = useTranslations();
  const label = t("menu.fibu.eInvoiceChecker");
  const href = toAbsoluteUrl(resolveMenuUrl("react/eInvoiceChecker/dynamic"));

  return (
    // The page's own description as the tooltip, so the button says what one can hand the checker: it
    // takes a file of *any* origin, not the invoice one came from (`fibu.eInvoiceChecker.description`).
    <HintTooltip text={t("fibu.eInvoiceChecker.description")}>
      <Button
        asChild
        type="button"
        variant="ghost"
        size="sm"
        className="gap-1.5"
      >
        <a href={href} target="_blank" rel="noopener noreferrer">
          <HugeiconsIcon icon={FileValidationIcon} size={14} aria-hidden />
          {label}
        </a>
      </Button>
    </HintTooltip>
  );
}
