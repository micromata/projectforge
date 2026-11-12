"use client";

import { useTranslations } from "next-intl";
import { ArrowLeft01Icon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import { Button } from "@/components/ui/button";

interface ViewToggleButtonProps {
  icon: IconSvgElement;
  /** The other view, e.g. "Grafiken". */
  label: string;
  /** The other view is showing: the button then leads back to the list. */
  pressed: boolean;
  onToggle: () => void;
}

/**
 * Switches a list page between its list and a second view of the same data (charts, a forecast). A button in the
 * head rather than a tab bar, so the page has the heading every other list has: it opens the second view, which
 * has a "back" in the same place. Icon only on a narrow head.
 */
export function ViewToggleButton({
  icon,
  label,
  pressed,
  onToggle,
}: ViewToggleButtonProps) {
  const t = useTranslations();
  const text = pressed ? t("back") : label;
  return (
    <Button
      type="button"
      variant="ghost"
      size="sm"
      aria-label={text}
      onClick={onToggle}
      className="gap-1.5"
    >
      <HugeiconsIcon
        icon={pressed ? ArrowLeft01Icon : icon}
        size={16}
        aria-hidden
      />
      <span className="@max-3xl:hidden">{text}</span>
    </Button>
  );
}
