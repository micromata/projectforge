"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { ColorPicker } from "@/components/shared/color-picker";
import { leafKeyOf } from "@/lib/leaf-key";

interface CalendarStylePopoverProps {
  title: string;
  visible: boolean;
  onToggleVisible: (visible: boolean) => void;
  color: string;
  onColorChange: (color: string) => void;
}

/**
 * The body of a calendar pill's popover: whether the calendar is shown, and in which colour. Purely
 * presentational — the visibility toggle takes effect at once (`setVisibility`), and the chosen colour
 * is reported up on every change; the pill applies it live, debounced (see calendar-pill).
 */
export function CalendarStylePopover({
  title,
  visible,
  onToggleVisible,
  color,
  onColorChange,
}: CalendarStylePopoverProps) {
  const t = useTranslations();

  return (
    <div className="flex w-56 flex-col gap-3">
      <p className="truncate text-sm font-semibold">{title}</p>
      <div className="flex items-center gap-2">
        <Checkbox
          id="calendar-style-visible"
          checked={visible}
          onCheckedChange={(value) => onToggleVisible(value === true)}
        />
        <Label htmlFor="calendar-style-visible" className="text-sm font-normal">
          {t("calendar.filter.visible")}
        </Label>
      </div>
      <Separator />
      <ColorPicker
        value={color}
        onChange={onColorChange}
        // `calendar.settings.colors` is a namespace as well as a label (its children are the scheme
        // and per-kind colours), so the bare key resolves to an object — leafKeyOf takes the `._` leaf.
        aria-label={t(leafKeyOf("calendar.settings.colors", t.has))}
      />
    </div>
  );
}
