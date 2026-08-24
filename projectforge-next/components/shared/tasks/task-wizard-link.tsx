"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { MagicWand01Icon } from "@hugeicons/core-free-icons";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import { TASK_WIZARD_ROUTE } from "./task-routes";

/**
 * The way to the access wizard, for admins as in Wicket (`TaskTreePage.init`) — its endpoints check
 * that as well, so this only hides a button that would answer 403.
 *
 * A button of its own rather than an entry of the gear menu, where it used to sit: it sets up the
 * rights of a whole project in one go, which is not maintenance and not something to go looking for.
 *
 * On both perspectives on the tasks, the tree and the list: what it does concerns the structure
 * elements, not the way they are looked at (see TaskPerspectiveLink, which is shared for the same
 * reason). Renders nothing for a non-admin, so either header may place it unconditionally.
 */
export function TaskWizardLink() {
  const t = useTranslations();
  const { isAdmin } = useAuth();
  if (!isAdmin) return null;

  return (
    <HintTooltip text={t("task.wizard.intro")}>
      <Button asChild variant="outline" size="sm" className="gap-1.5">
        <Link href={TASK_WIZARD_ROUTE}>
          <HugeiconsIcon icon={MagicWand01Icon} size={14} aria-hidden />
          {t("task.wizard.pageTitle")}
        </Link>
      </Button>
    </HintTooltip>
  );
}
