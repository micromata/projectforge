"use client";

import { RichText } from "@/components/shared/rich-text";
import { SCRIPT_PARAMETER_KEYS, type Script } from "@/lib/rs/script";
import { ScriptParameterInput } from "./script-parameter-input";

/**
 * The inputs of the script's declared parameters, in their order, each with its description (rich text,
 * or Markdown written before) behind the ⓘ of its label — wider than a footnote, it may be a paragraph.
 * A slot without a name or type declares nothing and is skipped.
 */
export function ScriptExecuteParameters({ script }: { script: Script }) {
  const declared = SCRIPT_PARAMETER_KEYS.flatMap((key) => {
    const param = script[key];
    return param?.name && param.type ? [{ key, param }] : [];
  });
  if (declared.length === 0) return null;
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {declared.map(({ key, param }) => (
        <ScriptParameterInput
          key={key}
          paramKey={key}
          param={param}
          hint={
            param.description ? (
              <RichText html={param.description} className="max-w-xl text-xs" />
            ) : undefined
          }
        />
      ))}
    </div>
  );
}
