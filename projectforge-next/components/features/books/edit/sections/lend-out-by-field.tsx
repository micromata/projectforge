"use client";

import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { useBookEditForm } from "../book-edit-context";
import type { UserRef } from "../../types";

interface Props {
  label: string;
}

export function LendOutByField({ label }: Props) {
  const form = useBookEditForm();
  return (
    <form.Field name={"lendOutBy" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const ref = field.state.value as UserRef | null;
        return (
          <Field className="gap-1.5">
            <FieldLabel className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
              {label}
            </FieldLabel>
            <Input
              value={ref?.displayName ?? ""}
              onChange={(e) => {
                const v = e.target.value.trim();
                if (!v) {
                  field.handleChange(null);
                  return;
                }
                field.handleChange({
                  id: ref?.id ?? -1,
                  displayName: e.target.value,
                });
              }}
              onBlur={field.handleBlur}
            />
          </Field>
        );
      }}
    </form.Field>
  );
}
