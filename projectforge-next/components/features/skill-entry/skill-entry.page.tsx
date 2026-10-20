import { SKILL_ENTRY_METADATA } from "@/lib/metadata/skill-entry.generated";
import { definePage } from "@/lib/page-def/define-page";
import {
  SkillField,
  SkillInterestField,
  SkillOwnerField,
  SkillRatingField,
} from "./skill-entry-fields";
import { SkillEntryListActions } from "./skill-entry-list-actions";
import { SkillStarsCell } from "./skill-stars-cell";
import {
  skillEntrySchema,
  SKILL_ENTRY_FIELDS,
  type SkillEntryValues,
} from "./skill-entry-schema";
import { emptySkillEntryValues, toFormValues } from "./skill-entry-values";
import type { SkillEntryDetail, SkillEntryListRow } from "./types";

/** React Query key of the list, so a write from the edit page refreshes it. */
export const SKILL_ENTRY_LIST_QUERY_KEY = ["skillentry"] as const;

/**
 * The whole page of the skill matrix plugin — list and edit — as data (see lib/page-def/types.ts).
 *
 * Replaces the removed React pages (`SkillEntryPagesRest`), which it follows: the list's columns in their
 * order with the ratings as stars, the filter "my skills" (on by default, declared by the backend), the Excel
 * export of the statistics; the form with the skill (completing), the owner and the two star ratings. Everybody
 * reads every skill, but writes only the own ones (`SkillRight`); a clone becomes the logged-in user's.
 */
export const SKILL_ENTRY_PAGE = definePage<
  SkillEntryListRow,
  SkillEntryValues,
  SkillEntryDetail,
  typeof SKILL_ENTRY_METADATA
>({
  entity: "skillentry",
  metadata: SKILL_ENTRY_METADATA,
  route: "/skillentry",
  queryKey: SKILL_ENTRY_LIST_QUERY_KEY,
  // Where the entry sits in the main menu: project management (SkillMatrixPlugin).
  categoryKey: "menu.projectmanagement",
  titleKey: "plugins.skillmatrix.title.list",
  defaultSort: { id: "lastUpdate", desc: true },
  columns: [
    { name: "lastUpdate", size: 130 },
    { name: "skill", size: 260, className: "font-semibold" },
    { name: "owner", size: 200 },
    {
      name: "rating",
      size: 110,
      cell: ({ row }) => (
        <SkillStarsCell value={row.original.rating} field="rating" />
      ),
    },
    {
      name: "interest",
      size: 110,
      cell: ({ row }) => (
        <SkillStarsCell value={row.original.interest} field="interest" />
      ),
    },
    { name: "comment", size: 400 },
  ],
  listActions: SkillEntryListActions,
  // Mass update of rating, interest and comment (SkillEntryMultiSelectedPageRest).
  massUpdate: {
    endpoint: "skillentrySelected",
    route: "/skillentry/mass-update",
  },
  edit: {
    schema: skillEntrySchema,
    fieldNames: SKILL_ENTRY_FIELDS,
    defaultValues: emptySkillEntryValues,
    toFormValues,
    title: (entry) => entry.skill ?? "",
    newTitleKey: "plugins.skillmatrix.title.add",
    savedMessageKey: "message.successfullChanged",
    // The copy is the logged-in user's (`SkillEntryEntityRest.prepareClone`): a colleague's skill taken over.
    clone: true,
    autoFocus: "skill",
    sections: [
      {
        id: "general",
        titleKey: "plugins.skillmatrix.skill",
        fields: [
          { custom: SkillField, span: 3 },
          { custom: SkillOwnerField },
          { custom: SkillRatingField },
          { custom: SkillInterestField },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
    ],
  },
});
