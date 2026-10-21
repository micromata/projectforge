"use client";

import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useMutation } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, BookOpen01Icon } from "@hugeicons/core-free-icons";
import { LogViewerLink } from "@/components/shared/log-viewer-link";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useListMeta } from "@/hooks/use-list-meta";
import { createMerlinExample, type MerlinExample } from "@/lib/rs/merlin";
import { toast } from "@/lib/toast";
import { MERLIN_DOCUMENTATION_URL, merlinExecuteRoute } from "./merlin-routes";

/**
 * The toolbar of the template list: the examples (a contract, a letter — each creates a template of the
 * user's own, with its Word® template, and opens its execution), the documentation of the templates and the
 * user's Merlin log.
 */
export function MerlinListActions() {
  const t = useTranslations();
  const router = useRouter();
  const logViewerUrl = useListMeta("merlin").data?.variables?.logViewerUrl as
    | string
    | undefined;
  const example = useMutation({
    mutationFn: (kind: MerlinExample) => createMerlinExample(kind),
    onSuccess: (id) => router.push(merlinExecuteRoute(id)),
    onError: (error) =>
      toast.error(error instanceof Error ? error.message : String(error)),
  });
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="sm"
            className="gap-1.5"
            disabled={example.isPending}
          >
            {t("plugins.merlin.menu.examples._")}
            <HugeiconsIcon icon={ArrowDown01Icon} size={14} aria-hidden />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onSelect={() => example.mutate("contract")}>
            {t("plugins.merlin.menu.examples.contract")}
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={() => example.mutate("letter")}>
            {t("plugins.merlin.menu.examples.letter")}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <Button asChild variant="ghost" size="sm" className="gap-1.5">
        <a
          href={MERLIN_DOCUMENTATION_URL}
          target="_blank"
          rel="noopener noreferrer"
        >
          <HugeiconsIcon icon={BookOpen01Icon} size={14} aria-hidden />
          {t("plugins.merlin.documentation")}
        </a>
      </Button>
      <LogViewerLink url={logViewerUrl} label={t("plugins.merlin.viewLogs")} />
    </>
  );
}
