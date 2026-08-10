"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft01Icon } from "@hugeicons/core-free-icons";
import { LegacyPageLink } from "@/components/shared/legacy-page-link";
import { StatusBadge } from "../status-badge";

export interface BookEditHeaderProps {
  title: string;
  lendOut: boolean;
  /** The legacy React edit page of this book, see LegacyPageLink. */
  legacyUrl?: string;
}

export function BookEditHeader({
  title,
  lendOut,
  legacyUrl,
}: BookEditHeaderProps) {
  const t = useTranslations("books.edit");
  return (
    <div className="flex h-11 items-center gap-2 overflow-hidden border-b border-border bg-background px-6">
      <Link
        href="/books"
        className="flex shrink-0 items-center gap-1 text-sm font-medium text-foreground/70 hover:text-foreground"
      >
        <HugeiconsIcon
          icon={ArrowLeft01Icon}
          size={14}
          className="text-muted-foreground"
        />
        <span>{t("breadcrumbBack")}</span>
      </Link>
      <span className="shrink-0 text-base text-border">/</span>
      <span className="min-w-0 flex-1 truncate text-sm text-muted-foreground">
        {title}
      </span>
      <StatusBadge
        lendOut={lendOut}
        label={lendOut ? t("status.loaned") : t("status.available")}
        variant="pill"
      />
      {/* Right, not left: the left of this row is the breadcrumb back to the list, and putting a
          second link beside it would read as part of that path. */}
      <LegacyPageLink url={legacyUrl} />
    </div>
  );
}
