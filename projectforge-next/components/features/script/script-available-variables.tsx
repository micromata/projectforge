"use client";

import { useTranslations } from "next-intl";
import { useEntityData } from "@/components/shared/form/form-context";
import type { ScriptDetail } from "./types";

/**
 * The variables a script finds bound when it runs (`ScriptExecution.getVariableNames`), as code.
 * [titled] false where a section heading already names them (the form, see ScriptAvailableVariablesField).
 */
export function ScriptAvailableVariables({
  variables,
  titled = true,
}: {
  variables: string | null | undefined;
  titled?: boolean;
}) {
  const t = useTranslations();
  if (!variables) return null;
  return (
    <div className="text-sm">
      {titled && (
        <div className="mb-1 font-medium">
          {t("scripting.script.availableVariables")}
        </div>
      )}
      <code className="block rounded-md bg-muted px-3 py-2 text-xs break-words whitespace-pre-wrap">
        {variables}
      </code>
    </div>
  );
}

/** The variables of the script in the form, as a section of their own (reachable by its tab). */
export function ScriptAvailableVariablesField({
  className,
}: {
  className?: string;
}) {
  const data = useEntityData<ScriptDetail>();
  return (
    <div className={className}>
      <ScriptAvailableVariables
        variables={data?.availableVariables}
        titled={false}
      />
    </div>
  );
}
