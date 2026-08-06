"use client";

import type { ReactNode } from "react";
import { useDynamicLayout } from "../dynamic-context";
import { Label } from "@/components/ui/label";
import type { DynamicLayoutNode } from "@/lib/rs/types";

/** The html id of a field. Prefixed with the layout's uid, so two layouts never collide. */
export function fieldDomId(uid: string | undefined, id: string): string {
  return uid ? `${uid}-${id}` : id;
}

interface DynamicFieldProps {
  node: DynamicLayoutNode;
  children: (domId: string, hasError: boolean) => ReactNode;
}

/**
 * Label, required marker, additional label, tooltip and validation error around one input.
 *
 * Every labelled element of the protocol (UILabelledElement) carries the same four fields, so all
 * input components share this frame and only contribute the control itself.
 */
export function DynamicField({ node, children }: DynamicFieldProps) {
  const { ui, translate, validationErrors } = useDynamicLayout();

  const id = node.id as string;
  const label = node.label as string | undefined;
  const additionalLabel = node.additionalLabel as string | undefined;
  const tooltip = node.tooltip as string | undefined;
  const required = node.required as boolean | undefined;

  const error = validationErrors.find((e) => e.fieldId === id);
  const domId = fieldDomId(ui.uid, id);

  return (
    <div className="flex min-w-0 flex-1 flex-col gap-1.5" title={tooltip}>
      {label && (
        <Label htmlFor={domId} className="text-sm">
          {translate(label)}
          {required && <span className="ml-0.5 text-destructive">*</span>}
          {additionalLabel && (
            <span className="ml-1 text-muted-foreground">
              {translate(additionalLabel)}
            </span>
          )}
        </Label>
      )}
      {children(domId, error != null)}
      {error && <p className="text-xs text-destructive">{error.message}</p>}
    </div>
  );
}
