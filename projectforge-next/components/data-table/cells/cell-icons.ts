import {
  Attachment01Icon,
  Cancel01Icon,
  InformationCircleIcon,
  StarIcon,
  Tick02Icon,
  UserLock01Icon,
} from "@hugeicons/core-free-icons";
import type { CellIconName } from "./cell-types";

/**
 * The single place mapping the backend's icon names onto Hugeicons.
 *
 * UIIconType serialises as a FontAwesome pair (`["fas", "check"]`), which is why
 * the adapter normalises the wire value to one of these names first (see
 * `iconNameFromWire`). Nothing outside this file needs to know about FontAwesome.
 */
export const CELL_ICONS = {
  checked: Tick02Icon,
  starRegular: StarIcon,
  userLock: UserLock01Icon,
  paperClip: Attachment01Icon,
  info: InformationCircleIcon,
  times: Cancel01Icon,
} as const satisfies Record<CellIconName, unknown>;

/** FontAwesome icon name (the pair's second element) → our semantic name. */
const WIRE_NAMES: Record<string, CellIconName> = {
  check: "checked",
  star: "starRegular",
  "user-lock": "userLock",
  paperclip: "paperClip",
  info: "info",
  times: "times",
};

/** Resolves a `UIIconType` as it arrives on the wire, e.g. `["far", "star"]`. */
export function iconNameFromWire(value: unknown): CellIconName | undefined {
  if (typeof value === "string") return WIRE_NAMES[value];
  if (Array.isArray(value)) {
    const last = value[value.length - 1];
    return typeof last === "string" ? WIRE_NAMES[last] : undefined;
  }
  return undefined;
}

/** Resolves a semantic icon name, or undefined for one we don't map (yet). */
export function cellIcon(name: string | undefined) {
  if (!name) return undefined;
  return CELL_ICONS[name as CellIconName];
}
