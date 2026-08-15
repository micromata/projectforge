"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { Command, CommandInput } from "@/components/ui/command";
import {
  Popover,
  PopoverAnchor,
  PopoverContent,
} from "@/components/ui/popover";
import { QuickAccessResults } from "@/components/shared/quick-access-results";

/**
 * The quick access search of the nav bar: click the magnifier, type, pick a menu entry below.
 *
 * The field is the nav's own, not a dialog's — searching the menu is a move within the navigation,
 * and a modal would take the whole page for it. The hits hang below the field as a popover, so what
 * is on screen stays visible while the user narrows the term.
 *
 * Closed it is the magnifier alone, which is all it takes to be recognised as a search: the
 * favourites next to it have more use for the width than a placeholder does. Open, the field unfolds
 * in its place and takes the focus, so the first keystroke is already part of the term.
 *
 * The `Command` wraps both halves although only the list is portalled: cmdk needs input and list in
 * one context — that is what makes the arrow keys and Enter of the field drive the list.
 *
 * Deliberately not a `MenubarMenu`, although it sits inside the nav's `Menubar`: an arrow key here
 * moves through the hits, not on to the next menu.
 */
export function QuickAccessSearch() {
  const t = useTranslations("menu");
  const [open, setOpen] = useState(false);
  const [term, setTerm] = useState("");

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (!(event.metaKey || event.ctrlKey)) return;
      // `event.code` names the physical key, so the shortcut survives a keyboard layout that puts
      // something else on `K` (see useAddEntryShortcut for the same reason on macOS).
      if (event.code !== "KeyK") return;
      // Unlike the bare `N` of the list pages this holds while text is being entered: a held
      // modifier is not typing, and reaching the next module from inside a filter field is the very
      // case the search is for.
      event.preventDefault();
      setOpen(true);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  function close() {
    setOpen(false);
    // Every visit starts with an empty field: the previous term answers a question already answered,
    // and it would hide the recents behind its own hits.
    setTerm("");
  }

  return (
    // `contents`: the cmdk root is needed as the shared context and keeps its key handling, but it
    // must not draw a box of its own around a nav item.
    <Command shouldFilter={false} className="contents">
      <Popover open={open} onOpenChange={(next) => !next && close()}>
        <PopoverAnchor asChild>
          <div className="shrink-0">
            {open ? (
              <CommandInput
                value={term}
                onValueChange={setTerm}
                placeholder={t("quickAccess.placeholder")}
                // Focused the moment it unfolds: the user clicked to type, and a field they have to
                // click a second time is worse than the button they just left.
                autoFocus
                // The placeholder alone would name the field only while it is empty. `._`: the key
                // is a text and the parent of others, so the catalog holds it as a subtree.
                aria-label={t("quickAccess._")}
                className="w-56"
              />
            ) : (
              <button
                type="button"
                onClick={() => setOpen(true)}
                aria-label={t("quickAccess._")}
                aria-keyshortcuts="Meta+K Control+K"
                className="flex h-7 w-8 cursor-pointer items-center justify-center rounded-md border border-input bg-input/20 text-muted-foreground hover:bg-accent hover:text-accent-foreground dark:bg-input/30"
              >
                <HugeiconsIcon icon={Search01Icon} size={14} />
              </button>
            )}
          </div>
        </PopoverAnchor>
        <PopoverContent
          align="start"
          // The field keeps the focus: the user is still typing, and the list is driven from there.
          onOpenAutoFocus={(event) => event.preventDefault()}
          className="max-h-[min(60vh,24rem)] w-80 overflow-y-auto p-0"
        >
          <QuickAccessResults term={term} onNavigate={close} />
        </PopoverContent>
      </Popover>
    </Command>
  );
}
