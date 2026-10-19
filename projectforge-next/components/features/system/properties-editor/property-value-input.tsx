"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { SecretInput } from "@/components/shared/secret-input";
import type { PropertyParam } from "@/lib/rs/properties-editor";
import { currentValue, type PropertyEdit } from "./properties-editor-model";

/**
 * The input of one parameter: a switch for booleans, a secret box for secrets (whose value is never
 * sent here, so it starts empty and only takes a new one), a text box otherwise. Clearing the box of
 * a parameter that isn't in the file drops the edit, rather than writing an empty value.
 */
export function PropertyValueInput({
  param,
  edit,
  onEdit,
  readOnly,
}: {
  param: PropertyParam;
  edit: PropertyEdit | undefined;
  onEdit: (edit: PropertyEdit | undefined) => void;
  readOnly: boolean;
}) {
  const t = useTranslations();
  const value = currentValue(param, edit);

  function change(newValue: string) {
    onEdit(
      !param.inFile && newValue === ""
        ? undefined
        : { kind: "set", value: newValue }
    );
  }

  if (param.secret) {
    return (
      <div className="flex items-center gap-2">
        <SecretInput
          className="min-w-0 flex-1"
          value={edit?.kind === "set" ? edit.value : ""}
          onChange={change}
          label={param.key}
          aria-label={param.key}
          copyable={false}
          disabled={readOnly}
        />
        <span className="shrink-0 text-xs text-muted-foreground">
          {edit?.kind === "set"
            ? t("system.propertiesEditor.secretChanged")
            : param.inFile && edit?.kind !== "remove"
              ? t("system.propertiesEditor.secret.set")
              : t("system.propertiesEditor.secret.notSet")}
        </span>
      </div>
    );
  }

  const effective = value ?? param.defaultValue ?? param.example ?? "";
  if (
    param.type === "BOOLEAN" &&
    (effective === "true" || effective === "false")
  ) {
    return (
      <Switch
        checked={effective === "true"}
        onCheckedChange={(checked) =>
          onEdit({ kind: "set", value: String(checked) })
        }
        aria-label={param.key}
        disabled={readOnly}
      />
    );
  }

  return (
    <Input
      className="h-8 w-full font-mono text-xs"
      value={value ?? ""}
      placeholder={
        param.defaultValue ||
        param.example ||
        t("system.propertiesEditor.unset")
      }
      aria-label={param.key}
      disabled={readOnly}
      onChange={(e) => change(e.target.value)}
    />
  );
}
