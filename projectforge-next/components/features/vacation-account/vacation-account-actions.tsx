"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  CalculatorIcon,
  FileExportIcon,
  LeftToRightListBulletIcon,
  PlusSignIcon,
  UserStatusIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { MenuLink } from "@/components/shared/menu-link";
import type { VacationAccount } from "./types";

/**
 * The actions of the vacation account: a new vacation of the employee shown, the remaining leave recalculated
 * (HR staff, only while the stored carry differs from what was left of the previous year), the vacation list, the
 * availability entries (the other absences) and the export of the team vacation calendars. While another employee's account loads (`switching`), the two
 * actions on the employee are disabled: they would still act on the one shown.
 */
export function VacationAccountActions({
  account,
  switching,
  recalculating,
  onRecalculate,
}: {
  account: VacationAccount | undefined;
  switching: boolean;
  recalculating: boolean;
  onRecalculate: () => void;
}) {
  const t = useTranslations();
  if (!account) return null;
  return (
    <>
      {account.canRecalculate && (
        <Button
          type="button"
          variant="destructive"
          size="sm"
          onClick={onRecalculate}
          disabled={recalculating || switching}
        >
          <HugeiconsIcon icon={CalculatorIcon} size={14} aria-hidden />
          {t("vacation.recalculateRemainingLeave")}
        </Button>
      )}
      <Button asChild variant="ghost" size="sm" className="gap-1.5">
        <MenuLink url={account.vacationListUrl}>
          <HugeiconsIcon
            icon={LeftToRightListBulletIcon}
            size={14}
            aria-hidden
          />
          {t("menu.vacation._")}
        </MenuLink>
      </Button>
      <Button asChild variant="ghost" size="sm" className="gap-1.5">
        <Link href="/availability">
          <HugeiconsIcon icon={UserStatusIcon} size={14} aria-hidden />
          {t("menu.availability")}
        </Link>
      </Button>
      <Button asChild size="sm" className="gap-1.5">
        <MenuLink url={account.exportUrl}>
          <HugeiconsIcon icon={FileExportIcon} size={14} aria-hidden />
          {t("vacation.export.title")}
        </MenuLink>
      </Button>
      {account.newVacationUrl &&
        (switching ? (
          <Button type="button" size="sm" className="gap-1.5" disabled>
            <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
            {t("vacation.title.add")}
          </Button>
        ) : (
          <Button asChild size="sm" className="gap-1.5">
            <MenuLink url={account.newVacationUrl}>
              <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
              {t("vacation.title.add")}
            </MenuLink>
          </Button>
        ))}
    </>
  );
}
