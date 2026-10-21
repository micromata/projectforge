"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useStore } from "@tanstack/react-form";
import { CodeDiffButton } from "@/components/shared/code-editor/code-diff-button";
import { useHistoryVersions } from "@/components/shared/code-editor/use-history-versions";
import {
  useEntityData,
  useEntityEditForm,
} from "@/components/shared/form/form-context";
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group";
import { isValidJson, prettyJson } from "@/lib/pretty-json";
import { cn } from "@/lib/utils";
import { CONFIGURATION_ENTITY, type ConfigurationDetail } from "../types";
import { ConfigurationJsonRawEditor } from "./configuration-json-raw-editor";
import { JSON_EDITORS, type JsonEditorProps } from "./json-editors";

type Mode = "form" | "json";

/**
 * The value of a JSON parameter: its form (JSON_EDITORS) or its JSON text, switched above, together with
 * the diff of the JSON against the stored value and the former ones of the history. A parameter without a
 * form is shown as JSON only. The mode is a view only, not stored; back to the form only from valid JSON.
 * Read-only, both views and the diff stay at hand, only the values are disabled.
 */
export function ConfigurationJsonValueField({
  parameter,
  readOnly,
  className,
  ...props
}: JsonEditorProps & { parameter: string | null; readOnly: boolean }) {
  const t = useTranslations("administration.configuration.json");
  const form = useEntityEditForm();
  const data = useEntityData<ConfigurationDetail>();
  const value = useStore(
    form.store,
    (s: unknown) =>
      (s as { values: { stringValue?: string | null } }).values.stringValue
  );
  const Editor = parameter ? JSON_EDITORS[parameter] : undefined;
  const [mode, setMode] = useState<Mode>(Editor ? "form" : "json");
  const [blocked, setBlocked] = useState(false);
  const versions = useHistoryVersions({
    entity: CONFIGURATION_ENTITY,
    id: data?.id,
    property: "stringValue",
    format: prettyJson,
  });
  const switchTo = (next: Mode) => {
    // The form can't show what doesn't parse; it would show nothing and overwrite it on the next change.
    if (next === "form" && !isValidJson(value)) {
      setBlocked(true);
      return;
    }
    setBlocked(false);
    setMode(next);
  };
  return (
    // One column no wider than the form: a long line of the JSON scrolls in the editor rather than widening
    // the column and pushing the switch out of sight.
    <div
      className={cn(
        "grid min-w-0 grid-cols-[minmax(0,1fr)] content-start gap-2",
        className
      )}
    >
      <div className="flex flex-wrap items-center justify-end gap-2">
        {blocked && (
          <span role="alert" className="text-xs text-destructive">
            {t("switchInvalid")}
          </span>
        )}
        {Editor && (
          <ToggleGroup
            type="single"
            variant="outline"
            size="sm"
            spacing={0}
            value={mode}
            aria-label={t("mode._")}
            // Radix reports "" when the active item is clicked again: keep the mode then.
            onValueChange={(next) => next && switchTo(next as Mode)}
          >
            <ToggleGroupItem value="form" className="h-6 px-2 text-xs">
              {t("mode.form")}
            </ToggleGroupItem>
            <ToggleGroupItem value="json" className="h-6 px-2 text-xs">
              {t("mode.json")}
            </ToggleGroupItem>
          </ToggleGroup>
        )}
        <CodeDiffButton
          original={prettyJson(data?.stringValue)}
          current={prettyJson(value)}
          language="json"
          versions={versions}
          labelled
        />
      </div>
      {mode === "form" && Editor ? (
        readOnly ? (
          <fieldset disabled className="contents">
            <Editor {...props} />
          </fieldset>
        ) : (
          <Editor {...props} />
        )
      ) : (
        <>
          <ConfigurationJsonRawEditor {...props} readOnly={readOnly} />
          {Editor && !readOnly && (
            <p className="text-xs text-muted-foreground">
              {t("unknownFields")}
            </p>
          )}
        </>
      )}
    </div>
  );
}
