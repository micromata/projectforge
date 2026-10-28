"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ValueCombobox } from "@/components/shared/value-combobox";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import { fetchMenuBadgeOptions } from "@/lib/rs/notification";
import type { LinkMode } from "./types";

const NAME = "linkMenuItem";
/** The two choices that are no menu item; menu item ids are upper case, so these can't collide. */
const DEFAULT = "default";
const NONE = "none";

/**
 * Where the "Open" button of a notification leads, one combobox for the three link modes: the page
 * of the rule type, no button at all, or one of the menu items (the options of the menu badge).
 */
export function LinkField({ className }: { className?: string }) {
  const t = useTranslations("notification.rule");
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  const { data } = useQuery({
    queryKey: ["notificationRule", "menuBadges"],
    queryFn: ({ signal }) => fetchMenuBadgeOptions(signal),
    staleTime: Infinity,
  });
  const options = [
    { value: DEFAULT, label: t("linkMode.DEFAULT") },
    { value: NONE, label: t("linkMode.NONE") },
    ...(data ?? []).map((option) => ({
      value: option.id,
      label: option.label,
    })),
  ];
  const label = t("link");
  return (
    <form.Field name={"linkMode" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(modeField: any) => (
        <form.Field name={NAME as never}>
          {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
          {(field: any) => {
            const meta = field.state.meta as FieldMetaState;
            const mode = modeField.state.value as LinkMode;
            const menuItem = field.state.value as string | null;
            const selected =
              mode === "NONE"
                ? NONE
                : mode === "MENU_ITEM" && menuItem
                  ? menuItem
                  : DEFAULT;
            return (
              <FieldShell
                name={NAME}
                label={label}
                invalid={meta.isTouched && !meta.isValid}
                errors={fieldErrors(meta, label)}
                className={className}
                ids={ids}
              >
                <ValueCombobox
                  id={ids.controlId}
                  aria-label={label}
                  options={options}
                  selected={[selected]}
                  onChange={(next) => {
                    // Picking the selected option again unselects it: back to the default.
                    const value = next[0] ?? DEFAULT;
                    const isMenuItem = value !== DEFAULT && value !== NONE;
                    modeField.handleChange(
                      value === NONE
                        ? "NONE"
                        : isMenuItem
                          ? "MENU_ITEM"
                          : "DEFAULT"
                    );
                    field.handleChange(isMenuItem ? value : null);
                    field.handleBlur();
                  }}
                />
              </FieldShell>
            );
          }}
        </form.Field>
      )}
    </form.Field>
  );
}
