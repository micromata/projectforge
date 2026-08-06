"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import type { DynamicComponentProps } from "../../dynamic-renderer";
import { useDynamicLayout } from "../../dynamic-context";
import { DynamicField } from "../dynamic-field";
import { Input } from "@/components/ui/input";
import {
  Popover,
  PopoverContent,
  PopoverAnchor,
} from "@/components/ui/popover";
import { getByPath } from "@/lib/dynamic/path";
import { fetchAutoCompletion } from "@/lib/rs/dynamic";
import { cn } from "@/lib/utils";

/** Characters to type before asking the server, matching AutoCompletion.minChars. */
const MIN_CHARS = 2;

/**
 * A free-text INPUT that suggests values the backend has already seen for this property
 * (`{category}/autocomplete?property=…`, see AbstractPagesRest.getAutoCompletionForProperty).
 *
 * The suggestions are a convenience only - anything the user types is a valid value.
 */
export function DynamicAutoCompleteInput({ node }: DynamicComponentProps) {
  const { data, setData } = useDynamicLayout();
  const [open, setOpen] = useState(false);

  const id = node.id as string;
  const url = node.autoCompletionUrl as string;
  const urlParams = node.autoCompletionUrlParams as
    | Record<string, string>
    | undefined;
  const raw = getByPath(data, id);
  const value = raw != null ? String(raw) : "";

  // The backend may want other fields of the form as context (e.g. the selected task).
  const params = Object.fromEntries(
    Object.entries(urlParams ?? {}).map(([param, path]) => [
      param,
      getByPath(data, path),
    ])
  );

  const { data: completions = [] } = useQuery({
    queryKey: ["autoCompletion", url, value, params],
    queryFn: ({ signal }) =>
      fetchAutoCompletion<string>(url, value, params, signal),
    enabled: open && value.length >= MIN_CHARS,
  });

  const suggestions = completions.filter((entry) => entry !== value);

  return (
    <DynamicField node={node}>
      {(domId, hasError) => (
        <Popover open={open && suggestions.length > 0} onOpenChange={setOpen}>
          <PopoverAnchor asChild>
            <Input
              id={domId}
              value={value}
              autoFocus={node.focus as boolean | undefined}
              maxLength={node.maxLength as number | undefined}
              required={node.required as boolean | undefined}
              // The suggestion list replaces the browser's own history dropdown.
              autoComplete="off"
              className={cn(hasError && "border-destructive")}
              onChange={(e) => {
                setData({ [id]: e.target.value });
                setOpen(true);
              }}
              onFocus={() => setOpen(true)}
            />
          </PopoverAnchor>
          <PopoverContent
            align="start"
            className="w-(--radix-popover-trigger-width) p-1"
            // Keep the caret in the input while the list is open.
            onOpenAutoFocus={(e) => e.preventDefault()}
          >
            {suggestions.map((entry) => (
              <button
                key={entry}
                type="button"
                className="block w-full rounded px-2 py-1 text-left text-sm hover:bg-muted"
                onClick={() => {
                  setData({ [id]: entry });
                  setOpen(false);
                }}
              >
                {entry}
              </button>
            ))}
          </PopoverContent>
        </Popover>
      )}
    </DynamicField>
  );
}
