"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PencilEdit02Icon } from "@hugeicons/core-free-icons";
import { LogViewerLink } from "@/components/shared/log-viewer-link";
import { Button } from "@/components/ui/button";
import { scriptEditRoute } from "./script-routes";

/**
 * What the administration's execution page offers beside the heading: the log of the scripting loggers
 * and, of a stored script, the way to its sources. Nothing on a user's execution page.
 */
export function ScriptExecuteHeaderActions({
  admin,
  id,
  logViewerUrl,
}: {
  admin: boolean;
  id: number | null;
  logViewerUrl: string | null | undefined;
}) {
  const t = useTranslations();
  if (!admin) return null;
  return (
    <>
      <LogViewerLink url={logViewerUrl} />
      {id != null && (
        <Button asChild size="sm" variant="outline">
          <Link href={scriptEditRoute(id)}>
            <HugeiconsIcon icon={PencilEdit02Icon} size={13} aria-hidden />
            {t("edit")}
          </Link>
        </Button>
      )}
    </>
  );
}
