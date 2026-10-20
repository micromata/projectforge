"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

/**
 * Adds a parameter unknown to application.properties (e.g. an own Spring setting); it is shown in the
 * group "Other" and written on save. Collapsed to a button until used.
 */
export function AddPropertyForm({
  onAdd,
}: {
  onAdd: (key: string, value: string) => string | null;
}) {
  const t = useTranslations();
  const [open, setOpen] = useState(false);
  const [key, setKey] = useState("");
  const [value, setValue] = useState("");
  const [error, setError] = useState<string | null>(null);

  if (!open) {
    return (
      <div>
        <Button size="sm" variant="outline" onClick={() => setOpen(true)}>
          {t("system.propertiesEditor.addParameter._")}
        </Button>
      </div>
    );
  }

  function add() {
    const result = onAdd(key.trim(), value);
    setError(result);
    if (!result) {
      setKey("");
      setValue("");
      setOpen(false);
    }
  }

  return (
    <div className="flex flex-col gap-1">
      <div className="flex flex-wrap items-center gap-2">
        <Input
          className="h-8 w-full font-mono text-xs sm:w-80"
          value={key}
          autoFocus
          placeholder={t("system.propertiesEditor.addParameter.key")}
          aria-label={t("system.propertiesEditor.addParameter.key")}
          aria-invalid={error ? true : undefined}
          onChange={(e) => setKey(e.target.value)}
        />
        <Input
          className="h-8 w-full font-mono text-xs sm:w-64"
          value={value}
          placeholder={t("system.propertiesEditor.addParameter.value")}
          aria-label={t("system.propertiesEditor.addParameter.value")}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && add()}
        />
        <Button size="sm" variant="outline" onClick={() => setOpen(false)}>
          {t("cancel")}
        </Button>
        <Button size="sm" disabled={!key.trim()} onClick={add}>
          {t("system.propertiesEditor.addParameter._")}
        </Button>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
