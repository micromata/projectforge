"use client";

import type { DynamicComponentProps } from "../../dynamic-renderer";
import { useDynamicLayout } from "../../dynamic-context";
import { DynamicField } from "../dynamic-field";
import { Input } from "@/components/ui/input";
import { getByPath } from "@/lib/dynamic/path";
import { cn } from "@/lib/utils";

interface DynamicTextInputProps extends DynamicComponentProps {
  /** The html input type; chosen by the resolver from the element's dataType. */
  inputType?: string;
}

/** A plain single-line INPUT: text, number, password. */
export function DynamicTextInput({
  node,
  inputType = "text",
}: DynamicTextInputProps) {
  const { data, setData } = useDynamicLayout();

  const id = node.id as string;
  const raw = getByPath(data, id);

  return (
    <DynamicField node={node}>
      {(domId, hasError) => (
        <Input
          id={domId}
          type={inputType}
          // A 0 must render as "0", so an explicit null check instead of `?? ""`.
          value={raw != null ? String(raw) : ""}
          autoFocus={node.focus as boolean | undefined}
          maxLength={node.maxLength as number | undefined}
          required={node.required as boolean | undefined}
          inputMode={
            node.inputMode as React.ComponentProps<"input">["inputMode"]
          }
          pattern={node.pattern as string | undefined}
          autoComplete={node.autoComplete as string | undefined}
          className={cn(hasError && "border-destructive")}
          onChange={(e) => setData({ [id]: e.target.value })}
        />
      )}
    </DynamicField>
  );
}
