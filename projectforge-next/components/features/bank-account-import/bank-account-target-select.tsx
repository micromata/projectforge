"use client";

import { useId } from "react";
import { useTranslations } from "next-intl";
import {
  EntityAutocomplete,
  type EntityRef,
} from "@/components/shared/entity-autocomplete";
import { Label } from "@/components/ui/label";
import { leafKeyOf } from "@/lib/leaf-key";

interface Props {
  value: EntityRef | null;
  onChange: (account: EntityRef | null) => void;
}

/**
 * The account the transactions are imported into, chosen before the drop: the file is read with its import
 * settings. Whether the user may write into it is the backend's decision (BankAccountRecordImportRest).
 */
export function BankAccountTargetSelect({ value, onChange }: Props) {
  const t = useTranslations();
  const id = useId();
  return (
    <div className="flex max-w-sm flex-col gap-1.5">
      <Label htmlFor={id} className="text-sm">
        {t(leafKeyOf("plugins.banking.account", t.has))}
      </Label>
      <EntityAutocomplete
        id={id}
        url="bankAccount/autosearch?search=:search"
        minChars={0}
        value={value}
        onChange={onChange}
      />
    </div>
  );
}
