"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/lib/utils";
import type { FilterElement } from "@/lib/rs/types";
import { FilterField } from "./filter-field";
import { withFilterValue, type FilterValues } from "./filter-value";

interface FilterFieldGridProps {
  elements: FilterElement[];
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  className?: string;
}

/** Every filter field the backend offers, in as many columns as the width allows. */
export function FilterFieldGrid({
  elements,
  values,
  onChange,
  className,
}: FilterFieldGridProps) {
  const t = useTranslations("filter");

  if (elements.length === 0) {
    return <p className="text-xs text-muted-foreground">{t("noFields")}</p>;
  }

  return (
    <div
      className={cn(
        "grid gap-x-6 gap-y-3 sm:grid-cols-2 lg:grid-cols-3",
        className
      )}
    >
      {elements.map((element) => (
        <div key={element.id} title={element.tooltip}>
          <FilterField
            element={element}
            value={values[element.id]}
            onChange={(value) =>
              onChange(withFilterValue(values, element.id, value))
            }
          />
        </div>
      ))}
    </div>
  );
}
