"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ValidityEntriesSection } from "@/components/shared/validity-entries/validity-entries-section";
import type {
  ValidityEntriesAdapter,
  ValidityValueField,
} from "@/components/shared/validity-entries/types";
import { useEntityDetail } from "@/hooks/use-entity-detail";
import { historyQueryKey } from "@/hooks/use-history";
import { EMPLOYEE_METADATA } from "@/lib/metadata/employee.generated";
import {
  deleteValidSinceAttr,
  insertValidSinceAttr,
  updateValidSinceAttr,
} from "@/lib/rs/employee-valid-since-attr";
import { EMPLOYEE_LIST_QUERY_KEY } from "../employee-query-key";
import type {
  EmployeeDetail,
  EmployeeValidSinceAttr,
  EmployeeValidSinceAttrType,
} from "../types";

const ADAPTER: ValidityEntriesAdapter<EmployeeValidSinceAttr> = {
  insert: insertValidSinceAttr,
  update: updateValidSinceAttr,
  remove: deleteValidSinceAttr,
};

interface TypeConfig {
  /** The list of the DTO holding the entries of this type. */
  entriesField:
    | "statusEntries"
    | "weeklyWorkingHoursEntries"
    | "annualLeaveEntries";
  titleKey: string;
  valueField: ValidityValueField;
}

/** The value columns and section titles of the legacy page (`EmployeePagesRest.createEditLayout`). */
const TYPES: Record<EmployeeValidSinceAttrType, TypeConfig> = {
  ANNUAL_LEAVE: {
    entriesField: "annualLeaveEntries",
    titleKey: "fibu.employee.urlaubstage",
    valueField: {
      name: "value",
      labelKey: "days",
      kind: "int",
      required: true,
    },
  },
  WEEKLY_HOURS: {
    entriesField: "weeklyWorkingHoursEntries",
    titleKey: "fibu.employee.wochenstunden",
    valueField: {
      name: "value",
      labelKey: "fibu.employee.sollstunden",
      kind: "decimal",
      required: true,
    },
  },
  STATUS: {
    entriesField: "statusEntries",
    titleKey: "fibu.employee.status._",
    valueField: {
      name: "value",
      labelKey: "status",
      kind: "select",
      options: EMPLOYEE_METADATA.fields.status.enumValues.map((v) => ({
        value: v.value,
        labelKey: v.i18nKey,
      })),
      required: true,
    },
  },
};

export function employeeValidSinceTitleKey(type: EmployeeValidSinceAttrType) {
  return TYPES[type].titleKey;
}

/**
 * The entries of one time-dependent attribute of a stored employee — the status, the weekly working hours
 * or the annual leave days, each valid since its date. Written through `employee/validSinceAttr/...` right
 * away, not with the form (see ValidityEntriesSection). The answer replaces the entries of the cached
 * employee (no refetch: it already is what the backend has); the list, whose status, hours and leave days
 * follow the entries, and the history, which shows the entries' changes too (`EmployeeDao.addOwnHistoryEntries`),
 * are refreshed.
 */
export function EmployeeValidSinceSection({
  id,
  type,
}: {
  id: number;
  type: EmployeeValidSinceAttrType;
}) {
  const t = useTranslations();
  const queryClient = useQueryClient();
  const employee = useEntityDetail<EmployeeDetail>("employee", id).data;
  const config = TYPES[type];

  return (
    <ValidityEntriesSection<EmployeeValidSinceAttr>
      entries={employee?.[config.entriesField] ?? []}
      dateField="validSince"
      valueFields={[config.valueField]}
      dialogTitle={t(config.titleKey)}
      newEntry={() => ({ id: null, type, employeeId: id })}
      adapter={ADAPTER}
      // `null` is "not asked" and reads as allowed (lib/rs/entity-access.ts); the endpoints check anyway.
      readOnly={employee?.writeAccess === false}
      onChanged={(entries) => {
        queryClient.setQueryData<EmployeeDetail>(["employee", id], (old) =>
          old ? { ...old, [config.entriesField]: entries } : old
        );
        // The list shows the values in force today. Not the employee itself: the answer above already is
        // what the backend has now, and a refetch would only read it a second time.
        void queryClient.invalidateQueries({
          queryKey: EMPLOYEE_LIST_QUERY_KEY,
          predicate: (query) => query.queryKey[1] !== id,
        });
        void queryClient.invalidateQueries({
          queryKey: historyQueryKey("employee", id),
        });
      }}
    />
  );
}
