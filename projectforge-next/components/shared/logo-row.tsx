"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import { useUIStore } from "@/store/ui-store";
import { CustomerLogo } from "@/components/shared/customer-logo";
import { DevelopmentMarker } from "@/components/shared/development-marker";
import { ProjectForgeLogo } from "@/components/shared/projectforge-logo";

interface LogoRowProps {
  /** False where the row should stay put — a page that does not scroll has nothing to yield to. */
  collapsible?: boolean;
}

/**
 * The strip above the brand stripe: the customer's logo on the left where one is configured, the
 * ProjectForge wordmark on the right.
 *
 * It scrolls away. Once the page's scroll column has moved past a small threshold the row collapses to
 * nothing and the content gains its height; it comes back when that column is at the very top again
 * (see hooks/use-collapse-on-scroll.ts, which every scroll container opts into). The toolbar and the
 * table's sticky header are unaffected either way — they live inside that column, not in this header.
 */
export function LogoRow({ collapsible = true }: LogoRowProps) {
  const t = useTranslations("logo");
  const collapsed = useUIStore((s) => s.logoCollapsed) && collapsible;

  return (
    <div
      className={cn(
        // A fixed height animating to 0, not `auto` or a max-height: `height` between two lengths is
        // the one that interpolates everywhere, and the content's height is known (the wordmark is
        // 43px, the customer logo is bounded to 36px). overflow-hidden is what makes the clipped
        // state read as a collapse instead of an overlap.
        "flex shrink-0 items-center justify-between overflow-hidden bg-background px-4",
        "transition-[height] duration-200 ease-out motion-reduce:transition-none",
        collapsed ? "h-0" : "h-12"
      )}
      // Nothing for a screen reader to announce once it is clipped away.
      aria-hidden={collapsed}
      // The state, readable from outside: aria-hidden takes the wordmark out of the accessibility
      // tree, so a test cannot find the collapsed row through the image (see e2e/logo-row.spec.ts).
      data-collapsed={collapsed}
    >
      <CustomerLogo />
      <DevelopmentMarker />
      {/* ml-auto keeps the wordmark right-aligned even with no customer logo on the left. */}
      <ProjectForgeLogo className="ml-auto" label={t("projectforge")} />
    </div>
  );
}
