"use client";

import { useTheme } from "next-themes";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Sun03Icon,
  Moon02Icon,
  ComputerIcon,
} from "@hugeicons/core-free-icons";
import {
  MenubarRadioGroup,
  MenubarRadioItem,
  MenubarSub,
  MenubarSubContent,
  MenubarSubTrigger,
} from "@/components/ui/menubar";
import { useSetThemePreference } from "@/hooks/use-theme-sync";
import type { ThemePreference } from "@/lib/rs/ui-settings";

const OPTIONS: { value: ThemePreference; icon: typeof Sun03Icon }[] = [
  { value: "light", icon: Sun03Icon },
  { value: "dark", icon: Moon02Icon },
  { value: "system", icon: ComputerIcon },
];

/**
 * Light/Dark/System submenu for the user menu. Applies the choice instantly via `next-themes` and persists it
 * per user (see useSetThemePreference). No hydration guard is needed: the menubar content is portalled and only
 * mounts once the user opens the menu, so this always renders client-side with a resolved `theme`.
 */
export function ThemeMenu() {
  const t = useTranslations("theme");
  const { theme } = useTheme();
  const setThemePreference = useSetThemePreference();

  const current = theme as ThemePreference | undefined;
  const triggerIcon =
    current === "light"
      ? Sun03Icon
      : current === "dark"
        ? Moon02Icon
        : ComputerIcon;

  return (
    <MenubarSub>
      <MenubarSubTrigger aria-label={t("label")}>
        <HugeiconsIcon icon={triggerIcon} />
        <span className="truncate">{t("label")}</span>
      </MenubarSubTrigger>
      <MenubarSubContent>
        <MenubarRadioGroup
          value={current}
          onValueChange={(value) =>
            setThemePreference(value as ThemePreference)
          }
        >
          {OPTIONS.map((option) => (
            <MenubarRadioItem key={option.value} value={option.value}>
              <HugeiconsIcon icon={option.icon} />
              {t(option.value)}
            </MenubarRadioItem>
          ))}
        </MenubarRadioGroup>
      </MenubarSubContent>
    </MenubarSub>
  );
}
