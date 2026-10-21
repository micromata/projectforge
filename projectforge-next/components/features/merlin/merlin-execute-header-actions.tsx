"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  Download04Icon,
  PencilEdit02Icon,
} from "@hugeicons/core-free-icons";
import { LogViewerLink } from "@/components/shared/log-viewer-link";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  downloadMerlinSerialTemplate,
  type MerlinExecution,
  type MerlinSerialFill,
} from "@/lib/rs/merlin";
import { toast } from "@/lib/toast";
import { merlinEditRoute } from "./merlin-routes";

/** The labels of the Excel® files of a serial execution, spelled out for the i18n scan. */
const FILL_KEYS: Record<MerlinSerialFill, string> = {
  base: "plugins.merlin.serial.template.download.base",
  employees: "plugins.merlin.serial.template.download.employees",
  users: "plugins.merlin.serial.template.download.users",
};

/**
 * Beside the heading of the execution: the Excel® files for a serial execution (empty, and for admins and
 * HR prefilled with the active employees or users), the Merlin log of the user and, for an admin of the
 * template, its form.
 */
export function MerlinExecuteHeaderActions({
  execution,
}: {
  execution: MerlinExecution | undefined;
}) {
  const t = useTranslations();
  const download = useMutation({
    mutationFn: (fill: MerlinSerialFill) =>
      downloadMerlinSerialTemplate(execution!.id, fill),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  if (!execution) return null;
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            size="sm"
            variant="outline"
            className="gap-1.5"
            disabled={download.isPending}
          >
            <HugeiconsIcon icon={Download04Icon} size={13} aria-hidden />
            {t("plugins.merlin.serial.template.download._")}
            <HugeiconsIcon icon={ArrowDown01Icon} size={13} aria-hidden />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {execution.serialFills.map((fill) => (
            <DropdownMenuItem key={fill} onSelect={() => download.mutate(fill)}>
              {t(FILL_KEYS[fill])}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
      <LogViewerLink
        url={execution.logViewerUrl}
        label={t("plugins.merlin.viewLogs")}
      />
      {execution.editAccess && (
        <Button asChild size="sm" variant="outline">
          <Link href={merlinEditRoute(execution.id)}>
            <HugeiconsIcon icon={PencilEdit02Icon} size={13} aria-hidden />
            {t("edit")}
          </Link>
        </Button>
      )}
    </>
  );
}
