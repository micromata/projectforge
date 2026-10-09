"use client";

import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon, PlayIcon } from "@hugeicons/core-free-icons";
import { GuardedLink } from "@/components/shared/guarded-link";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { fetchScriptExamples } from "@/lib/rs/script";
import { SCRIPT_EXECUTE_ROUTE } from "./script-routes";

/**
 * The toolbar of the script list: code executed without storing it (`/script/execute`), empty or
 * starting with one of the example scripts.
 */
export function ScriptListActions() {
  const t = useTranslations();
  const examples = useQuery({
    queryKey: ["script", "examples"],
    queryFn: ({ signal }) => fetchScriptExamples(signal),
    staleTime: Infinity,
  });
  return (
    <>
      <Button asChild variant="ghost" size="sm" className="gap-1.5">
        <GuardedLink href={SCRIPT_EXECUTE_ROUTE}>
          <HugeiconsIcon icon={PlayIcon} size={14} aria-hidden />
          {t("execute")}
        </GuardedLink>
      </Button>
      {(examples.data?.length ?? 0) > 0 && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="gap-1.5">
              {t("scripting.script.examples")}
              <HugeiconsIcon icon={ArrowDown01Icon} size={14} aria-hidden />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            {examples.data?.map((example) => (
              <DropdownMenuItem key={example.index} asChild>
                <GuardedLink
                  href={`${SCRIPT_EXECUTE_ROUTE}?example=${example.index}`}
                >
                  {example.title}
                </GuardedLink>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </>
  );
}
