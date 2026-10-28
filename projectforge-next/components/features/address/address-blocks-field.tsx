"use client";

import { useState } from "react";
import { useStore } from "@tanstack/react-form";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon, PlusSignIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { cn } from "@/lib/utils";
import {
  AddressBlockFields,
  ADDRESS_KINDS,
  fieldNamesOf,
  type AddressKind,
  type AddressKindDef,
} from "./address-block-fields";
import type { AddressValues } from "./address-schema";

/**
 * The business, postal and private address in one fieldset. Only the kinds holding a value are shown (so one
 * filled in by the text parser or the vCard comparison appears by itself), further ones are added through the
 * picker; a new address starts with the business one. Removing a kind clears its fields, saved with the form.
 */
export function AddressBlocksField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const filled = useStore(form.store, (s: unknown) => {
    const values = (s as { values: AddressValues }).values;
    return ADDRESS_KINDS.filter((def) =>
      fieldNamesOf(def).some((name) => {
        const value = values[name];
        return typeof value === "string" && value.trim() !== "";
      })
    )
      .map((def) => def.kind)
      .join(",");
  });
  const isNew = useStore(
    form.store,
    (s: unknown) => (s as { values: AddressValues }).values.id == null
  );
  const [added, setAdded] = useState<ReadonlySet<AddressKind>>(
    () => new Set(isNew ? ["business"] : [])
  );
  const shown = ADDRESS_KINDS.filter(
    (def) => added.has(def.kind) || filled.split(",").includes(def.kind)
  );
  const missing = ADDRESS_KINDS.filter((def) => !shown.includes(def));

  const add = (kind: AddressKind) =>
    setAdded((prev) => new Set(prev).add(kind));
  const remove = (def: AddressKindDef) => {
    for (const name of fieldNamesOf(def)) {
      form.setFieldValue(name as never, null as never);
    }
    setAdded((prev) => {
      const next = new Set(prev);
      next.delete(def.kind);
      return next;
    });
  };

  return (
    <div className={cn("flex flex-col gap-6", className)}>
      {/* Two addresses side by side from `md`, each a column of its own. */}
      <div className="grid gap-6 md:grid-cols-2">
        {shown.map((def) => (
          <fieldset
            key={def.kind}
            aria-label={t(def.headingKey)}
            className="flex flex-col gap-3"
          >
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-semibold">{t(def.headingKey)}</h3>
              {!readOnly && (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  aria-label={`${t("address.removeAddress")}: ${t(def.headingKey)}`}
                  onClick={() => remove(def)}
                >
                  <HugeiconsIcon icon={Delete02Icon} size={14} aria-hidden />
                </Button>
              )}
            </div>
            <AddressBlockFields def={def} />
          </fieldset>
        ))}
      </div>
      {!readOnly && missing.length > 0 && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="self-start"
            >
              <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
              {t("address.addAddress")}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            {missing.map((def) => (
              <DropdownMenuItem key={def.kind} onSelect={() => add(def.kind)}>
                {t(def.headingKey)}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
    </div>
  );
}
