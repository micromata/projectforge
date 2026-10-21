"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { PlusSignIcon } from "@hugeicons/core-free-icons";
import { useStore } from "@tanstack/react-form";
import { Button } from "@/components/ui/button";
import {
  useEntityEditForm,
  useFormReadOnly,
} from "@/components/shared/form/form-context";
import { FieldHint } from "@/components/shared/form/field-hint";
import { MarkdownText } from "@/components/shared/markdown-text";
import type { MerlinVariable } from "@/lib/rs/merlin";
import { cn } from "@/lib/utils";
import { MerlinVariableDialog } from "./merlin-variable-dialog";
import { MerlinVariableTable } from "./merlin-variable-table";
import { useMerlinVariables } from "./use-merlin-variables";

function Heading({ label, hint }: { label: string; hint: string }) {
  return (
    <div className="flex items-center gap-1 text-sm font-medium">
      {label}
      <FieldHint hint={hint} label={label} />
    </div>
  );
}

/**
 * The variables of the template: the input variables (entered on the execution page), the dependent ones
 * (derived from a master variable) and the conditionals of the Word® template. The variables come from the
 * Word® template and from what is configured here; a row or "add" opens the variable dialog.
 */
export function MerlinVariablesField({ className }: { className?: string }) {
  const t = useTranslations();
  const form = useEntityEditForm();
  const readOnly = useFormReadOnly();
  const { variables, dependentVariables } = useMerlinVariables();
  const conditionals = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (s: any) => s.values.conditionalsMarkdown as string | null
  );
  const [open, setOpen] = useState<MerlinVariable | null>(null);
  const onOpen = readOnly ? undefined : setOpen;
  return (
    <div className={cn("flex flex-col gap-4", className)}>
      <Heading
        label={t("plugins.merlin.variables.input._")}
        hint={t("plugins.merlin.variables.input.info")}
      />
      <MerlinVariableTable rows={variables} dependent={false} onOpen={onOpen} />
      <Heading
        label={t("plugins.merlin.variables.dependant._")}
        hint={t("plugins.merlin.variables.dependant.info")}
      />
      <MerlinVariableTable
        rows={dependentVariables}
        dependent
        onOpen={onOpen}
      />
      {!readOnly && (
        <div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setOpen({ name: "", type: "STRING" })}
          >
            <HugeiconsIcon icon={PlusSignIcon} size={14} aria-hidden />
            {t("plugins.merlin.variable.add")}
          </Button>
        </div>
      )}
      {conditionals && (
        <div className="flex flex-col gap-1">
          <div className="text-sm font-medium">
            {t("plugins.merlin.variables.conditionals")}
          </div>
          <MarkdownText
            className="rounded-md border bg-muted/40 p-3 text-sm"
            text={conditionals}
          />
        </div>
      )}
      {open && (
        <MerlinVariableDialog variable={open} onClose={() => setOpen(null)} />
      )}
    </div>
  );
}
