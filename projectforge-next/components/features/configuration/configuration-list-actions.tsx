"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowDown01Icon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { DescribedMenuItem } from "@/components/shared/described-menu-item";
import { useAuth } from "@/hooks/use-auth";
import { PropertiesDiffDialog } from "@/components/features/system/properties-editor/properties-diff-dialog";
import { PropertiesEditorDialog } from "@/components/features/system/properties-editor/properties-editor-dialog";

/**
 * The editor of projectforge.properties and the comparison with its backups, as on the System page
 * (see SystemMenuBar). Admin-group only, like the endpoints behind it; for everybody else nothing shows.
 */
export function ConfigurationListActions() {
  const t = useTranslations();
  const { isAdmin } = useAuth();
  const [editorOpen, setEditorOpen] = useState(false);
  const [diffOpen, setDiffOpen] = useState(false);

  if (!isAdmin) return null;
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="outline" size="sm" className="gap-1">
            projectforge.properties
            <HugeiconsIcon icon={ArrowDown01Icon} size={14} />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-80">
          <DescribedMenuItem
            label={t("system.propertiesEditor.title")}
            description={t("system.propertiesEditor.cardInfo")}
            onSelect={() => setEditorOpen(true)}
          />
          <DescribedMenuItem
            label={t("system.propertiesEditor.diff.title")}
            description={t("system.propertiesEditor.diff.info")}
            onSelect={() => setDiffOpen(true)}
          />
        </DropdownMenuContent>
      </DropdownMenu>
      {editorOpen && (
        <PropertiesEditorDialog onClose={() => setEditorOpen(false)} />
      )}
      {diffOpen && <PropertiesDiffDialog onClose={() => setDiffOpen(false)} />}
    </>
  );
}
