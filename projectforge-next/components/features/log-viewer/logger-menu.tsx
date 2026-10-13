"use client";

import { useTranslations } from "next-intl";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

/** Narrows the search down to a logger pattern or leaves it out. */
export type OnLogger = (pattern: string, mode: "include" | "exclude") => void;

/**
 * The logger of an entry, with the actions narrowing the search to it or its package, or hiding them as noise,
 * below where the entry was found. Plain text (where on hover) without [onLogger].
 */
export function LoggerMenu({
  logger,
  source,
  onLogger,
}: {
  logger: string;
  source: string;
  onLogger?: OnLogger;
}) {
  const t = useTranslations();
  if (!onLogger) {
    return (
      <HintTooltip text={source} plain openOnTap>
        <span className="[overflow-wrap:anywhere]">{logger}</span>
      </HintTooltip>
    );
  }
  const pkg = logger.includes(".")
    ? logger.substring(0, logger.lastIndexOf("."))
    : null;
  const patterns = pkg ? [logger, pkg] : [logger];
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="cursor-pointer text-left [overflow-wrap:anywhere] underline decoration-dotted underline-offset-2">
        {logger}
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        <DropdownMenuLabel className="font-normal text-muted-foreground">
          {source}
        </DropdownMenuLabel>
        {patterns.map((pattern) => (
          <DropdownMenuItem
            key={`include-${pattern}`}
            onSelect={() => onLogger(pattern, "include")}
          >
            {t("system.admin.adminLogViewer.logFile.onlyThis", {
              arg0: pattern,
            })}
          </DropdownMenuItem>
        ))}
        <DropdownMenuSeparator />
        {patterns.map((pattern) => (
          <DropdownMenuItem
            key={`exclude-${pattern}`}
            onSelect={() => onLogger(pattern, "exclude")}
          >
            {t("system.admin.adminLogViewer.logFile.hideThis", {
              arg0: pattern,
            })}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
