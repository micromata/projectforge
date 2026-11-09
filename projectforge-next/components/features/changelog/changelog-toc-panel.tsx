"use client";

import { type ComponentProps, useState, useSyncExternalStore } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  LeftToRightListBulletIcon,
  PanelRightCloseIcon,
  PanelRightOpenIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { ChangelogToc } from "./changelog-toc";

/** The local storage key remembering a hidden column (`hidden`), as `pf-doc-toc` on the website. */
const STORAGE_KEY = "pf.changelog.toc";

type TocProps = Omit<ComponentProps<typeof ChangelogToc>, "className">;

const listeners = new Set<() => void>();

function readHidden(): boolean {
  try {
    return window.localStorage.getItem(STORAGE_KEY) === "hidden";
  } catch {
    return false;
  }
}

function storeHidden(hidden: boolean) {
  try {
    if (hidden) window.localStorage.setItem(STORAGE_KEY, "hidden");
    else window.localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Without storage the choice is lost, the column stays shown.
  }
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/**
 * The table of contents as a column beside the changelog (from xl), sticking to the top of the scroll column.
 * It can be hidden to a narrow button showing it again; the choice is kept in the local storage.
 */
export function ChangelogTocColumn(props: TocProps) {
  const t = useTranslations();
  // Shown in the static export, which renders without storage.
  const hidden = useSyncExternalStore(subscribe, readHidden, () => false);
  const toggle = storeHidden;

  if (hidden) {
    return (
      <aside className="sticky top-2 hidden shrink-0 self-start pt-2 xl:block">
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={() => toggle(false)}
          aria-label={t("changelog.toc.show")}
        >
          <HugeiconsIcon icon={PanelRightOpenIcon} />
        </Button>
      </aside>
    );
  }
  return (
    <aside className="sticky top-0 hidden max-h-[calc(100dvh-10rem)] w-60 shrink-0 flex-col self-start pt-2 xl:flex">
      <div className="mb-1 flex items-center justify-between gap-2 px-2">
        <h2 className="text-xs font-bold uppercase tracking-wide text-muted-foreground">
          {t("changelog.toc.title")}
        </h2>
        <Button
          variant="ghost"
          size="icon-sm"
          onClick={() => toggle(true)}
          aria-label={t("changelog.toc.hide")}
        >
          <HugeiconsIcon icon={PanelRightCloseIcon} />
        </Button>
      </div>
      <ChangelogToc {...props} className="min-h-0 flex-1 pb-4" />
    </aside>
  );
}

/** Below xl (and in the app): a button of the title row opening the table of contents as a drawer. */
export function ChangelogTocSheet({ onNavigate, ...props }: TocProps) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button variant="outline" size="sm" className="xl:hidden">
          <HugeiconsIcon icon={LeftToRightListBulletIcon} />
          {t("changelog.toc.title")}
        </Button>
      </SheetTrigger>
      <SheetContent side="right" className="w-72">
        <SheetHeader className="pb-2">
          <SheetTitle>{t("changelog.toc.title")}</SheetTitle>
        </SheetHeader>
        <ChangelogToc
          {...props}
          onNavigate={(id) => {
            setOpen(false);
            // After the drawer gave the focus back, which would scroll to its button otherwise.
            window.setTimeout(() => onNavigate(id), 0);
          }}
          className="min-h-0 flex-1 px-4 pb-6"
        />
      </SheetContent>
    </Sheet>
  );
}
