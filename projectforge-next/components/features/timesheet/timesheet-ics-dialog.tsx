"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { Copy01Icon, TickDouble01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/shared/spinner";
import { fetchTimesheetIcsUrl } from "@/lib/rs/timesheet";

/** React Query key of the ics subscription url; the logged-in user's, so no id is part of it. */
const ICS_URL_QUERY_KEY = ["timesheet", "icsExportUrl"] as const;

/**
 * The "ics export" of the time sheet list: the subscription url of the calendar feed for the user to add
 * to their calendar app. A url, not a download — it carries the user's personal, encrypted token, hence
 * the security note (`calendar.icsExport.securityAdvice`, as the legacy `TimesheetsICSExportDialog`).
 */
export function TimesheetIcsDialog({ onClose }: { onClose: () => void }) {
  const t = useTranslations();
  const [copied, setCopied] = useState(false);
  const query = useQuery({
    queryKey: ICS_URL_QUERY_KEY,
    queryFn: ({ signal }) => fetchTimesheetIcsUrl(undefined, signal),
  });
  const url = query.data?.url;

  async function copy() {
    if (!url) return;
    await navigator.clipboard.writeText(url);
    setCopied(true);
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("timesheet.icsExport")}</DialogTitle>
          <DialogDescription>
            {t("timesheet.iCalSubscription")}
          </DialogDescription>
        </DialogHeader>
        {query.isLoading ? (
          <div className="flex justify-center py-4">
            <Spinner className="h-5 w-5 border-2" />
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Input
              readOnly
              value={url ?? ""}
              aria-label={t("timesheet.icsExport")}
            />
            <Button
              type="button"
              variant="outline"
              size="icon"
              className="shrink-0"
              aria-label={t("copy")}
              disabled={!url}
              onClick={() => void copy()}
            >
              <HugeiconsIcon
                icon={copied ? TickDouble01Icon : Copy01Icon}
                size={16}
              />
            </Button>
          </div>
        )}
        {/* The token in the url is a login token; warn as the legacy dialog does. */}
        <p className="text-xs text-muted-foreground">
          {t("calendar.icsExport.securityAdvice")}
        </p>
      </DialogContent>
    </Dialog>
  );
}
