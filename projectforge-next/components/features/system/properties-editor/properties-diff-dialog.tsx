"use client";

import { useId, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { CodeDiffDialog } from "@/components/shared/code-editor/code-diff-dialog";
import type { CodeVersionSource } from "@/components/shared/code-editor/types";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  fetchPropertiesBackups,
  fetchPropertiesDiffText,
} from "@/lib/rs/properties-editor";

/**
 * Compares projectforge.properties with its backups (written by the properties editor before every
 * save) or two backups with each other, in the diff of the code editor. By default the order is
 * ignored: the backend sorts the parameters, each with its comments, so the re-sorting done by the
 * editor drops out and only changed values and comments remain. Values of secrets are masked by the
 * backend; a fingerprint behind the mask still shows whether one changed.
 */
export function PropertiesDiffDialog({ onClose }: { onClose: () => void }) {
  const t = useTranslations();
  const ignoreOrderId = useId();
  const [ignoreOrder, setIgnoreOrder] = useState(true);
  const current = useQuery({
    queryKey: ["propertiesEditor", "diff", ignoreOrder, "current"],
    queryFn: ({ signal }) => fetchPropertiesDiffText(null, ignoreOrder, signal),
  });
  // The list of backups is fetched per mode, too: the diff caches it under the source's key.
  const versions = useMemo<CodeVersionSource>(
    () => ({
      queryKey: ["propertiesEditor", "diff", ignoreOrder],
      list: fetchPropertiesBackups,
      load: (key, signal) => fetchPropertiesDiffText(key, ignoreOrder, signal),
    }),
    [ignoreOrder]
  );
  return (
    <CodeDiffDialog
      open
      onOpenChange={(open) => !open && onClose()}
      title={t("system.propertiesEditor.diff.title")}
      original={current.data}
      current={current.data ?? ""}
      language="properties"
      versions={versions}
      storedLabel={t("system.propertiesEditor.diff.current")}
      options={
        <div className="flex items-center gap-2">
          <Switch
            id={ignoreOrderId}
            checked={ignoreOrder}
            onCheckedChange={setIgnoreOrder}
          />
          <Label htmlFor={ignoreOrderId}>
            {t("system.propertiesEditor.diff.ignoreOrder")}
          </Label>
        </div>
      }
    />
  );
}
