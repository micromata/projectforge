"use client";

import { useId } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  ArrowDown01Icon,
  ArrowUp01Icon,
  Delete02Icon,
  PlusSignIcon,
} from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { FieldError } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import {
  FieldShell,
  useFieldIds,
  type FieldMetaState,
} from "@/components/shared/form/field-shell";
import { useEntityEditForm } from "@/components/shared/form/form-context";
import { useFieldErrors } from "@/components/shared/form/use-field-errors";
import {
  EMPTY_JIRA_SERVER,
  parseJiraConfig,
  serializeJiraConfig,
  type JiraConfigValue,
  type JiraServerEntry,
} from "../jira-config";
import { ColumnHeader } from "./column-header";
import type { JsonEditorProps } from "./json-editors";

// Fixed width of the actions (3 icon buttons of 1.75rem): the header row has no buttons, so an auto column
// would shift its headers against the inputs.
const COLUMNS = "grid grid-cols-[minmax(16rem,1fr)_minmax(10rem,0.6fr)_5.25rem] items-center gap-2";

/**
 * The editor of the parameter `jira`: the default browse url and one row per further server (browse url and
 * projects). Every change writes the whole object back; the backend's errors arrive on `stringValue` and are
 * listed above the fields.
 */
export function JiraConfigEditor({ label, hint, className }: JsonEditorProps) {
  const t = useTranslations();
  const browseBaseUrlId = useId();
  const form = useEntityEditForm();
  const fieldErrors = useFieldErrors();
  const ids = useFieldIds();
  return (
    <form.Field name={"stringValue" as never}>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {(field: any) => {
        const meta = field.state.meta as FieldMetaState;
        const invalid = !meta.isValid;
        const errors = fieldErrors(meta, label);
        const { config, invalid: invalidJson } = parseJiraConfig(
          field.state.value as string | null
        );
        const entries = config.servers;
        const writeConfig = (next: JiraConfigValue) => field.handleChange(serializeJiraConfig(next));
        const write = (servers: JiraServerEntry[]) => writeConfig({ ...config, servers });
        const set = (index: number, patch: Partial<JiraServerEntry>) =>
          write(entries.map((entry, i) => (i === index ? { ...entry, ...patch } : entry)));
        const move = (index: number, delta: number) => {
          const target = index + delta;
          if (target < 0 || target >= entries.length) return;
          const next = [...entries];
          [next[index], next[target]] = [next[target], next[index]];
          write(next);
        };
        return (
          <FieldShell
            name="stringValue"
            label={label}
            hint={hint}
            invalid={invalid}
            errors={[]}
            className={className}
            ids={ids}
          >
            <div
              id={ids.controlId}
              role="group"
              aria-labelledby={ids.labelId}
              className="grid gap-3 rounded-md border p-3"
            >
              {invalid && errors.length > 0 && (
                <div className="grid gap-1">
                  {errors.map((error) => (
                    <FieldError key={error}>{error}</FieldError>
                  ))}
                </div>
              )}
              {invalidJson && (
                <p className="text-xs text-warning">
                  {t("administration.configuration.jira.invalidJson")}
                </p>
              )}
              <div className="grid max-w-4xl gap-1">
                <label
                  htmlFor={browseBaseUrlId}
                  className="text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground"
                >
                  <ColumnHeader
                    label={t("administration.configuration.jira.browseBaseUrl")}
                    hint={t("administration.configuration.jira.help.browseBaseUrl")}
                  />
                </label>
                <Input
                  id={browseBaseUrlId}
                  value={config.browseBaseUrl}
                  placeholder="https://jira.acme.com/jira/browse/"
                  onChange={(e) => writeConfig({ ...config, browseBaseUrl: e.target.value })}
                  onBlur={field.handleBlur}
                />
              </div>
              <div className="text-sm font-medium">
                <ColumnHeader
                  label={t("administration.configuration.jira.servers")}
                  hint={t("administration.configuration.jira.help.servers")}
                />
              </div>
              <div className="grid max-w-4xl gap-2">
                <div
                  className={`${COLUMNS} text-[11.5px] font-semibold uppercase tracking-wide text-muted-foreground`}
                >
                  <ColumnHeader
                    label={t("administration.configuration.jira.baseUrl")}
                    hint={t("administration.configuration.jira.help.baseUrl")}
                  />
                  <ColumnHeader
                    label={t("administration.configuration.jira.projects")}
                    hint={t("administration.configuration.jira.help.projects")}
                  />
                  <span />
                </div>
                {entries.map((entry, index) => {
                  const rowLabel = entry.projects.trim() || entry.baseUrl.trim() || `${index + 1}`;
                  return (
                    <div
                      // The rows have no identity of their own; the index is what the JSON array has.
                      key={index}
                      className={COLUMNS}
                    >
                      <Input
                        value={entry.baseUrl}
                        placeholder="https://customer.acme.com/jira/browse/"
                        aria-label={`${t("administration.configuration.jira.baseUrl")}: ${rowLabel}`}
                        onChange={(e) => set(index, { baseUrl: e.target.value })}
                        onBlur={field.handleBlur}
                      />
                      <Input
                        value={entry.projects}
                        placeholder="ACME, ADMIN"
                        aria-label={`${t("administration.configuration.jira.projects")}: ${rowLabel}`}
                        onChange={(e) => set(index, { projects: e.target.value })}
                        onBlur={field.handleBlur}
                      />
                      <div className="flex">
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          disabled={index === 0}
                          aria-label={t("administration.configuration.jira.moveUp", { arg0: rowLabel })}
                          onClick={() => move(index, -1)}
                        >
                          <HugeiconsIcon icon={ArrowUp01Icon} className="h-4 w-4" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          disabled={index === entries.length - 1}
                          aria-label={t("administration.configuration.jira.moveDown", { arg0: rowLabel })}
                          onClick={() => move(index, 1)}
                        >
                          <HugeiconsIcon icon={ArrowDown01Icon} className="h-4 w-4" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          aria-label={t("administration.configuration.jira.remove", { arg0: rowLabel })}
                          onClick={() => write(entries.filter((_, i) => i !== index))}
                        >
                          <HugeiconsIcon icon={Delete02Icon} className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  );
                })}
              </div>
              <div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => write([...entries, EMPTY_JIRA_SERVER])}
                >
                  <HugeiconsIcon icon={PlusSignIcon} className="h-4 w-4" />
                  {t("administration.configuration.jira.add")}
                </Button>
              </div>
            </div>
          </FieldShell>
        );
      }}
    </form.Field>
  );
}
