"use client";

import type { ComponentType } from "react";
import type { DynamicComponentProps } from "../dynamic-renderer";
import { DynamicFallback } from "../components/dynamic-fallback";
import { ColorChooser } from "./color-chooser";

/**
 * A `UICustomized` element's own props: its `values` map (`UICustomized.values`), which carries the
 * field id and whatever the specific customised type needs (a colour chooser's label and default).
 */
export interface CustomizedComponentProps {
  values: Record<string, unknown>;
}

/**
 * The registry of `UICustomized` types (`node.type === "CUSTOMIZED"`). Unlike every other element the
 * renderer dispatches on the node's `type`, these all share that one type and are told apart by their
 * `id` (`UICustomized.TYPE.id`, e.g. `"color-chooser"`) — the one place the frontend must know a
 * customised element by name. An unknown id falls through to the same dev-visible placeholder as any
 * other unimplemented element.
 */
const CUSTOMIZED_MAP: Record<
  string,
  ComponentType<CustomizedComponentProps>
> = {
  "color-chooser": ColorChooser,
};

export function DynamicCustomized({ node }: DynamicComponentProps) {
  const id = node.id as string | undefined;
  const Component = id ? CUSTOMIZED_MAP[id] : undefined;
  if (!Component) return <DynamicFallback node={node} />;
  const values = (node.values as Record<string, unknown> | undefined) ?? {};
  return <Component values={values} />;
}
