"use client";

import { useState } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import { Search01Icon } from "@hugeicons/core-free-icons";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import type { FilterElement } from "@/lib/rs/types";
import { FilterFieldGroup } from "./filter-field-group";
import {
  ACTIVE_GROUP_ID,
  TECHNICAL_GROUP_ID,
  buildFilterGroups,
  filterGroupsBySearch,
  matchesSearchTerm,
  searchTextsOf,
} from "./filter-groups";
import type { FilterValues } from "./filter-value";
import {
  historyFilterGroupOf,
  mergeHistoryFilters,
  pickHistoryFilters,
  withoutHistoryFilters,
} from "./history-filter";
import { HistoryFilterFields } from "./history-filter-fields";

interface FilterAllDialogProps {
  elements: FilterElement[];
  /** The applied filters; the dialog edits a copy of them. */
  initial: FilterValues;
  onApply: (values: FilterValues) => void;
  onClose: () => void;
}

/**
 * Every filter field of the list at once — the overview the pill row can't give.
 *
 * A list offers every search field of its entity, which is a flat wall of 40 for an order, so the
 * fields are grouped ([buildFilterGroups]) and all but the first groups start closed. The search
 * narrows across all of them and opens whatever survives.
 *
 * Edits a draft and only applies on demand: each applied change is a new query key, so filtering per
 * keystroke would refetch the list while the dialog is still open. Mount this only while it is open,
 * so every draft starts from the applied filters.
 */
export function FilterAllDialog({
  elements,
  initial,
  onApply,
  onClose,
}: FilterAllDialogProps) {
  const t = useTranslations("filter");
  const tAction = useTranslations();
  const [draft, setDraft] = useState(initial);
  const [term, setTerm] = useState("");
  const [closed, setClosed] = useState<Set<string>>(new Set());
  const [opened, setOpened] = useState<Set<string>>(new Set());

  const history = historyFilterGroupOf(elements);
  const groups = filterGroupsBySearch(
    buildFilterGroups(withoutHistoryFilters(elements), draft),
    term
  );
  // Its own section, above the groups and never collapsed: three inputs for the one question every
  // entity can be asked, and the one filter the pill row groups the same way.
  const showHistory =
    history != null &&
    (matchesSearchTerm(term, t("history")) ||
      Object.values(history).some((element) =>
        matchesSearchTerm(term, ...searchTextsOf(element))
      ));

  function headingOf(id: string, groupLabel: string | null) {
    if (groupLabel) return groupLabel;
    if (id === ACTIVE_GROUP_ID) return t("activeFilters");
    if (id === TECHNICAL_GROUP_ID) return t("moreFields");
    return t("generalFields");
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="grid max-h-[85vh] w-[min(1100px,calc(100vw-2rem))] grid-rows-[auto_auto_1fr_auto] gap-3 sm:max-w-none">
        <DialogHeader>
          <DialogTitle>{t("allFilters")}</DialogTitle>
        </DialogHeader>
        <div className="relative">
          <HugeiconsIcon
            icon={Search01Icon}
            size={14}
            className="pointer-events-none absolute top-1/2 left-2.5 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            value={term}
            onChange={(event) => setTerm(event.target.value)}
            placeholder={t("search")}
            aria-label={t("search")}
            className="h-8 pl-8 text-xs"
          />
        </div>
        <div className="min-h-0 space-y-1 overflow-y-auto pr-1">
          {showHistory && history && (
            <div className="space-y-1.5 px-2 pb-2">
              <p className="text-xs font-semibold">{t("history")}</p>
              <div className="grid auto-rows-min items-start gap-x-6 gap-y-3 sm:grid-cols-2 lg:grid-cols-3">
                <HistoryFilterFields
                  group={history}
                  values={pickHistoryFilters(draft)}
                  onChange={(next) =>
                    setDraft(mergeHistoryFilters(draft, next))
                  }
                />
              </div>
            </div>
          )}
          {groups.map((group) => (
            <FilterFieldGroup
              key={group.id}
              group={group}
              label={headingOf(group.id, group.groupLabel)}
              values={draft}
              onChange={setDraft}
              // A search opens every group it left standing; clearing it returns to the user's own
              // open and closed ones.
              open={
                term
                  ? true
                  : opened.has(group.id) ||
                    (group.defaultOpen && !closed.has(group.id))
              }
              onOpenChange={(open) => toggle(group.id, open)}
            />
          ))}
          {groups.length === 0 && !showHistory && (
            <p className="px-2 py-4 text-xs text-muted-foreground">
              {elements.length === 0 ? t("noFields") : t("noMatch")}
            </p>
          )}
        </div>
        <DialogFooter className="border-t pt-3">
          <Button
            variant="ghost"
            size="sm"
            className="sm:mr-auto"
            onClick={() => setDraft({})}
          >
            {t("reset")}
          </Button>
          <Button variant="outline" size="sm" onClick={onClose}>
            {tAction("cancel")}
          </Button>
          <Button size="sm" onClick={() => onApply(draft)}>
            {tAction("apply")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );

  /** Kept as the two things a user did, so a group's default survives a search. */
  function toggle(id: string, open: boolean) {
    setOpened(withMember(opened, id, open));
    setClosed(withMember(closed, id, !open));
  }
}

function withMember(
  ids: Set<string>,
  id: string,
  member: boolean
): Set<string> {
  const next = new Set(ids);
  if (member) next.add(id);
  else next.delete(id);
  return next;
}
