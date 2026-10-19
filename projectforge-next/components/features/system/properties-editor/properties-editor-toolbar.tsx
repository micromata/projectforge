"use client";

import { useTranslations } from "next-intl";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { AddPropertyForm } from "./add-property-form";
import type { PropertyFilter } from "./properties-editor-model";

/** Search (key, description, own comments), the filters "set in file" / "modified" and "add". */
export function PropertiesEditorToolbar({
  filter,
  onFilterChange,
  onAdd,
  disabled,
}: {
  filter: PropertyFilter;
  onFilterChange: (filter: PropertyFilter) => void;
  onAdd: (key: string, value: string) => string | null;
  disabled: boolean;
}) {
  const t = useTranslations();
  return (
    <div className="flex flex-col gap-2 pt-1">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        <Input
          type="search"
          className="h-8 w-full sm:w-80"
          value={filter.query}
          placeholder={t("system.propertiesEditor.search")}
          aria-label={t("system.propertiesEditor.search")}
          onChange={(e) => onFilterChange({ ...filter, query: e.target.value })}
        />
        <FilterCheckbox
          id="properties-only-in-file"
          label={t("system.propertiesEditor.filter.inFile")}
          checked={filter.onlyInFile}
          onChange={(checked) =>
            onFilterChange({ ...filter, onlyInFile: checked })
          }
        />
        <FilterCheckbox
          id="properties-only-modified"
          label={t("system.propertiesEditor.filter.modified")}
          checked={filter.onlyModified}
          onChange={(checked) =>
            onFilterChange({ ...filter, onlyModified: checked })
          }
        />
      </div>
      {!disabled && <AddPropertyForm onAdd={onAdd} />}
    </div>
  );
}

function FilterCheckbox({
  id,
  label,
  checked,
  onChange,
}: {
  id: string;
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-center gap-2">
      <Checkbox
        id={id}
        checked={checked}
        onCheckedChange={(value) => onChange(value === true)}
      />
      <Label htmlFor={id} className="text-sm font-normal">
        {label}
      </Label>
    </div>
  );
}
