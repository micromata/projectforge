"use client";

import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { FilterIcon, Search01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { type TaskTreeFilter } from "@/lib/rs/task";

/** The four flags, in the order the legacy panel lists them, with their i18n keys. */
const STATUS_FLAGS = [
  { name: "opened", key: "task.status.opened" },
  { name: "notOpened", key: "task.status.notOpened" },
  { name: "closed", key: "task.status.closed" },
  { name: "deleted", key: "deleted" },
] as const satisfies readonly { name: keyof TaskTreeFilter; key: string }[];

interface TaskTreeFilterBarProps {
  filter: TaskTreeFilter;
  onChange: (filter: TaskTreeFilter) => void;
}

/**
 * Search field and the status flags of the structure tree.
 *
 * The flags sit in a popover rather than beside the field: four checkboxes are the rarely used part,
 * and the trigger lists the ones that are *on* — so the collapsed state still tells the whole filter,
 * which is what the legacy panel achieved by writing them into the field's text. Naming the deviation
 * from the default instead would read backwards: an unchecked box would appear in the pill.
 */
export function TaskTreeFilterBar({
  filter,
  onChange,
}: TaskTreeFilterBarProps) {
  const t = useTranslations();
  const active = STATUS_FLAGS.filter((flag) => filter[flag.name] === true);

  return (
    <div className="flex items-center gap-2">
      <div className="relative max-w-md flex-1">
        <HugeiconsIcon
          icon={Search01Icon}
          size={14}
          className="absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground"
          aria-hidden
        />
        <Input
          value={filter.searchString}
          onChange={(e) =>
            onChange({ ...filter, searchString: e.target.value })
          }
          placeholder={t("search._")}
          aria-label={t("search._")}
          className="h-8 pl-8 text-xs"
        />
      </div>
      <Popover>
        <PopoverTrigger asChild>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-8 gap-1.5 text-xs"
          >
            <HugeiconsIcon icon={FilterIcon} size={13} aria-hidden />
            {/* Unchecking everything is a filter that matches nothing; the generic label keeps the
                trigger from collapsing to just its icon. */}
            {active.length > 0
              ? active.map((flag) => t(flag.key)).join(", ")
              : t("status")}
          </Button>
        </PopoverTrigger>
        <PopoverContent align="end" className="w-56 space-y-2 p-3">
          {STATUS_FLAGS.map((flag) => (
            <div key={flag.name} className="flex items-center gap-2">
              <Checkbox
                id={`task-filter-${flag.name}`}
                checked={filter[flag.name] === true}
                onCheckedChange={(checked) =>
                  onChange({ ...filter, [flag.name]: checked === true })
                }
              />
              <Label
                htmlFor={`task-filter-${flag.name}`}
                className="text-xs font-normal"
              >
                {t(flag.key)}
              </Label>
            </div>
          ))}
        </PopoverContent>
      </Popover>
    </div>
  );
}
