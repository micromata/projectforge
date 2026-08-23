"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { SmileIcon, WinkIcon } from "@hugeicons/core-free-icons";
import { leafKeyOf } from "@/lib/leaf-key";
import type { EntityRef } from "./entity-autocomplete";

export interface SelectMeButtonProps {
  /** The logged-in user as a reference, see [useCurrentUserRef]. */
  me: EntityRef;
  onPick: (me: EntityRef) => void;
}

/**
 * Picks the logged-in user with one click, beside a field that asks for a person — the „select me"
 * smiley of the legacy UserSelect.jsx, winking while the pointer is on it.
 *
 * The caller decides when it makes sense at all: it is hidden where the user is already the value, and
 * a field asking for a project or a cost unit has no use for it (see [EntityAutocompleteField]).
 */
export function SelectMeButton({ me, onPick }: SelectMeButtonProps) {
  const t = useTranslations();
  const [winking, setWinking] = useState(false);
  return (
    <button
      type="button"
      // On pointer down, as the reset button beside it: the button disappears the moment it is used,
      // so a popover around it would take the missing pointerup for a click outside itself.
      onPointerDown={(e) => {
        e.preventDefault();
        onPick(me);
      }}
      onPointerEnter={() => setWinking(true)}
      onPointerLeave={() => setWinking(false)}
      onFocus={() => setWinking(true)}
      onBlur={() => setWinking(false)}
      // The tooltip of the legacy smiley is a joke („You are great!"), which names nothing — the
      // accessible name says what the button does, the joke stays as the title.
      aria-label={`${t(leafKeyOf("select", t.has))}: ${me.displayName}`}
      title={t("tooltip.selectMe")}
      className="shrink-0 cursor-pointer text-muted-foreground hover:text-foreground"
    >
      <HugeiconsIcon icon={winking ? WinkIcon : SmileIcon} size={16} />
    </button>
  );
}
