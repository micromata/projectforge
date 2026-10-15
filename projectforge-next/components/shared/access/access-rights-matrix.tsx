import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import {
  CheckmarkCircle02Icon,
  Delete02Icon,
  Edit02Icon,
  MinusSignCircleIcon,
  PlusSignIcon,
  ViewIcon,
} from "@hugeicons/core-free-icons";
import type { AccessRight, AccessTypeName } from "@/lib/rs/task";
import { cn } from "@/lib/utils";

/** The name of an access type, kept as literals so the i18n key scanner sees them. */
export const ACCESS_TYPE_KEYS: Record<AccessTypeName, string> = {
  TASK_ACCESS_MANAGEMENT: "access.type.accessManagement",
  TASKS: "access.type.tasks",
  TIMESHEETS: "access.type.timesheets",
  OWN_TIMESHEETS: "access.type.ownTimesheets",
};

/** The four permissions of an access type, in the order the access management shows them. */
const OPERATIONS: {
  key: keyof Omit<AccessRight, "accessType">;
  labelKey: string;
  icon: IconSvgElement;
}[] = [
  { key: "select", labelKey: "access.type.select", icon: ViewIcon },
  { key: "insert", labelKey: "access.type.insert", icon: PlusSignIcon },
  { key: "update", labelKey: "access.type.update", icon: Edit02Icon },
  { key: "delete", labelKey: "access.type.delete", icon: Delete02Icon },
];

/** One line of the matrix, so it lines up with the labels beside it (see AccessTypeLegend). */
const LINE = "flex h-4 items-center gap-1";

export interface AccessRightsMatrixProps {
  rights: AccessRight[];
  /** Translator for the whole bundle, i.e. `useTranslations()` — the keys here are absolute. */
  t: (key: string) => string;
  className?: string;
}

/**
 * The permissions of one access entry as the Wicket access panel shows them: one line per access type,
 * a green check or a red minus per permission.
 *
 * Without labels of its own, so it can be repeated in the cells of a table: what the lines and the
 * columns are is said once, by [AccessTypeLegend] beside it and [AccessOperationsHeader] above it. Every
 * icon carries the whole statement as its accessible name nevertheless, because a row of icons is no
 * text.
 */
export function AccessRightsMatrix({
  rights,
  t,
  className,
}: AccessRightsMatrixProps) {
  return (
    <div className={cn("flex flex-col", className)}>
      {rights.map((right) => (
        <div key={right.accessType} className={LINE}>
          {OPERATIONS.map((operation) => {
            const granted = right[operation.key];
            return (
              <HugeiconsIcon
                key={operation.key}
                icon={granted ? CheckmarkCircle02Icon : MinusSignCircleIcon}
                size={13}
                role="img"
                aria-label={`${t(ACCESS_TYPE_KEYS[right.accessType])}, ${t(
                  operation.labelKey
                )}: ${t(
                  granted
                    ? "access.permission.granted"
                    : "access.permission.denied"
                )}`}
                className={cn(
                  "shrink-0",
                  granted ? "text-emerald-600" : "text-destructive"
                )}
              />
            );
          })}
        </div>
      ))}
    </div>
  );
}

/** The names of the access types, one per line of the matrix next to it. */
export function AccessTypeLegend({
  rights,
  t,
  className,
}: AccessRightsMatrixProps) {
  return (
    <div className={cn("flex flex-col", className)}>
      {rights.map((right) => (
        <span
          key={right.accessType}
          className={cn(LINE, "truncate text-muted-foreground")}
        >
          {t(ACCESS_TYPE_KEYS[right.accessType])}
        </span>
      ))}
    </div>
  );
}

/** The four permission icons, naming the columns of every matrix below them. */
export function AccessOperationsHeader({
  t,
  className,
}: Omit<AccessRightsMatrixProps, "rights">) {
  return (
    <div className={cn(LINE, className)}>
      {OPERATIONS.map((operation) => (
        <HugeiconsIcon
          key={operation.key}
          icon={operation.icon}
          size={13}
          role="img"
          aria-label={t(operation.labelKey)}
          data-tooltip={t(operation.labelKey)}
          className="shrink-0 text-muted-foreground"
        />
      ))}
    </div>
  );
}
