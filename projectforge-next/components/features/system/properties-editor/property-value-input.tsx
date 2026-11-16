"use client";

import { useTranslations } from "next-intl";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { CopyButton } from "@/components/shared/copy-button";
import { SecretInput } from "@/components/shared/secret-input";
import type { PropertyParam } from "@/lib/rs/properties-editor";
import { parseListEntryKey } from "./list-entries";
import { currentValue, type PropertyEdit } from "./properties-editor-model";

/**
 * The input of one parameter: a switch for booleans, a secret box for secrets (whose value is never
 * sent here, so it starts empty and only takes a new one), a text box otherwise, with a button
 * copying its default (or example) to the clipboard. Clearing the box of
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
    if (newValue !== "") return onEdit({ kind: "set", value: newValue });
    // An empty field of a list entry is no value: it is left out of the file, never written empty.
    if (!param.inFile) return onEdit(undefined);
    onEdit(
      parseListEntryKey(param.key)
        ? { kind: "remove" }
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

  // The default (or else the example) shown as placeholder can be copied, e.g. to adapt a long list.
  const suggestion = param.defaultValue || param.example;
  return (
    <div className="flex min-w-0 items-center gap-1">
      <Input
        className="h-8 w-full min-w-0 font-mono text-xs"
        value={value ?? ""}
        placeholder={suggestion || t("system.propertiesEditor.unset")}
        aria-label={param.key}
        disabled={readOnly}
        onChange={(e) => change(e.target.value)}
      />
      {/* Without one, the space is kept, so the boxes of all rows stay aligned. */}
      {!suggestion ? (
        <span className="size-8 shrink-0" aria-hidden />
      ) : (
        <CopyButton
          value={suggestion}
          label={
            param.defaultValue
              ? t("system.propertiesEditor.default")
              : t("system.propertiesEditor.example")
          }
          variant="ghost"
          className="size-8"
          iconSize={14}
        />
      )}
    </div>
  );
}
