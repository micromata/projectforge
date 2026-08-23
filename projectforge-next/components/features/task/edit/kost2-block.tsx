"use client";

import { useStore } from "@tanstack/react-form";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { EntityAutocomplete } from "@/components/shared/entity-autocomplete";
import { HintTooltip } from "@/components/shared/hint-tooltip";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { InputField } from "@/components/shared/form/input-field";
import { SelectField } from "@/components/shared/form/select-field";
import { useFieldLabels } from "@/components/shared/form/use-field-labels";
import { FieldLabel } from "@/components/ui/field";
import { TASK_METADATA } from "@/lib/metadata/task.generated";
import { fetchTaskInfo } from "@/lib/rs/task";
import { cn } from "@/lib/utils";
import type { TaskValues } from "../task-schema";
import { useKost2Preview } from "./use-kost2-preview";

/**
 * Which cost units may be booked on the task: the black or white list, its type, and a picker that
 * appends one — the `fibu.kost2` fieldset of the Wicket form, with its resolved list as a tooltip.
 *
 * Everything derived is the server's answer (see [useKost2Preview]); this only shows it and binds the
 * two form values. The picker is narrowed to the project's own cost units, as Wicket narrows its select
 * page (`nummer:<kost>.*`).
 *
 * @param id null while the task is being added — then the project is the parent's, which is also what
 *   Wicket resolves (`TaskEditForm.onBeforeRender`).
 * @param writeAccess whether this user may change the list at all (see FinanceSection).
 */
export function Kost2Block({
  id,
  writeAccess,
  className,
}: {
  id: number | null;
  writeAccess: boolean;
  className?: string;
}) {
  const t = useTranslations();
  const label = useFieldLabels(TASK_METADATA);
  const form = useEntityEditForm();
  // The parent, for a task that has no id yet: the only task there is to resolve a project from.
  const parentTaskId = useStore(
    form.store,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (state: any) => (state.values as TaskValues).parentTask?.id ?? null
  ) as number | null;
  const infoId = id ?? parentTaskId;

  // Same query as everywhere a task is shown by its id (TaskChip, TaskSelectField), so this is a cache
  // read wherever the tree has already asked. Wanted here for the project and for `costConfigured`,
  // neither of which the preview carries as an id.
  const { data: info } = useQuery({
    queryKey: ["taskInfo", infoId],
    queryFn: ({ signal }) => fetchTaskInfo(infoId!, signal),
    enabled: infoId != null,
    staleTime: Infinity,
  });
  const { preview, addKost2 } = useKost2Preview(id);

  // Hidden only where the installation is known not to use cost units — while nothing is known yet
  // (a new task without a parent) the fieldset is offered, as Wicket builds it from the same setting
  // rather than from the task.
  if (info?.costConfigured === false) return null;

  const projektKost = preview?.projektKost ?? info?.projekt?.kost ?? null;
  return (
    <div
      className={cn(
        "grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-3",
        className
      )}
    >
      <div className="flex flex-col gap-1.5 md:col-span-3">
        <FieldLabel className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
          {t("fibu.kost2._")}
        </FieldLabel>
        {/* The resolved cost units as the tooltip of the project's wild card, as in Wicket — the dash
            where the list matches none of them, so hovering says "none" instead of nothing. */}
        <HintTooltip plain text={preview?.kost2ListAsLines ?? " - (-)"}>
          <span className="w-fit font-mono text-sm">
            {projektKost ? `${projektKost}.*` : "—"}
          </span>
        </HintTooltip>
      </div>
      <InputField
        name="kost2BlackWhiteList"
        label={label("kost2BlackWhiteList")}
        disabled={!writeAccess}
        hint={writeAccess ? undefined : t("task.error.kost2Readonly")}
      />
      <SelectField
        name="kost2IsBlackList"
        label={label("kost2IsBlackList")}
        // White first and never clearable, as Wicket adds them (`setNullValid(false)`): a list is one
        // or the other, and the entity's field is a primitive `Boolean`.
        options={[
          { value: "false", label: t("task.kost2list.whiteList") },
          { value: "true", label: t("task.kost2list.blackList") },
        ]}
        valueType="boolean"
        clearable={false}
        disabled={!writeAccess}
      />
      {writeAccess && (
        <div className="flex flex-col gap-1.5">
          <FieldLabel className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground">
            {t("add")}
          </FieldLabel>
          {/* Not a form field: what is picked here is appended to the list above by the server and is
              no value of its own, so it is the context-free picker rather than an autocomplete field.
              `value` stays null — the box is empty again after each pick. */}
          <EntityAutocomplete
            aria-label={`${t("add")}: ${t("fibu.kost2._")}`}
            url="cost2/autosearch?search=:search"
            params={{ projektId: info?.projekt?.id ?? null }}
            value={null}
            onChange={(picked) => picked && addKost2(picked.id)}
          />
        </div>
      )}
    </div>
  );
}
