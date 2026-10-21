import { VISITORBOOK_METADATA } from "@/lib/metadata/visitorbook.generated";
import { definePage } from "@/lib/page-def/define-page";
import { VisitorbookContactPersonsField } from "./edit/visitorbook-contact-persons-field";
import { VisitorbookVisitsSection } from "./edit/visitorbook-visits-section";
import { VISITORBOOK_LIST_QUERY_KEY } from "./visitorbook-query-key";
import {
  visitorbookSchema,
  VISITORBOOK_FIELDS,
  type VisitorbookValues,
} from "./visitorbook-schema";
import { emptyVisitorbookValues, toFormValues } from "./visitorbook-values";
import type { VisitorbookDetail, VisitorbookListRow } from "./types";

/**
 * The visitor book (`VisitorbookEntityRest`), migrated from the React UILayout page: the list with the latest visit
 * of each visitor, and the form with the visits of a stored visitor, entered in a dialog.
 */
export const VISITORBOOK_PAGE = definePage<
  VisitorbookListRow,
  VisitorbookValues,
  VisitorbookDetail,
  typeof VISITORBOOK_METADATA
>({
  entity: "visitorbook",
  metadata: VISITORBOOK_METADATA,
  route: "/visitorbook",
  queryKey: VISITORBOOK_LIST_QUERY_KEY,
  categoryKey: "menu.orga",
  titleKey: "orga.visitorbook.title.list",
  // The latest visit first, as the legacy list (VisitorbookDao.select without a sort).
  defaultSort: { id: "lastDateOfVisit", desc: true },
  // The columns of the legacy list, in its order. The latest visit is computed from the visits
  // (VisitorbookEntityRest.computedSortProperties), the contact persons are joined names, hence not sortable.
  columns: [
    { name: "lastname", size: 160, className: "font-semibold" },
    { name: "firstname", size: 160 },
    { name: "company", size: 200 },
    { name: "visitortype", size: 140 },
    {
      id: "lastDateOfVisit",
      labelKey: "orga.visitorbook.lastVisit",
      accessor: (row) => row.lastDateOfVisit ?? null,
      dataType: "DATE",
      size: 130,
    },
    {
      id: "latestArrived",
      labelKey: "orga.visitorbook.lastVisit.arrived",
      accessor: (row) => row.latestArrived ?? null,
      size: 130,
    },
    {
      id: "latestDeparted",
      labelKey: "orga.visitorbook.lastVisit.departed",
      accessor: (row) => row.latestDeparted ?? null,
      size: 130,
    },
    {
      id: "numberOfVisits",
      labelKey: "orga.visitorbook.numberOfVisits",
      accessor: (row) => row.numberOfVisits ?? null,
      align: "right",
      size: 110,
    },
    {
      id: "contactPersonsAsString",
      labelKey: "orga.visitorbook.contactPersons",
      accessor: (row) => row.contactPersonsAsString ?? null,
      sortable: false,
      size: 220,
    },
    { name: "comment", size: 300, wrap: true },
    { name: "created", size: 130, hiddenByDefault: true },
    { name: "lastUpdate", size: 130, hiddenByDefault: true },
  ],
  edit: {
    schema: visitorbookSchema,
    fieldNames: VISITORBOOK_FIELDS,
    defaultValues: emptyVisitorbookValues,
    toFormValues,
    title: (visitor) =>
      [visitor.firstname, visitor.lastname].filter(Boolean).join(" "),
    newTitleKey: "orga.visitorbook.title.add",
    savedMessageKey: "message.successfullChanged",
    sections: [
      {
        id: "general",
        titleKey: "orga.visitorbook.title.heading",
        fields: [
          { name: "firstname" },
          { name: "lastname" },
          { name: "company" },
          { name: "visitortype" },
          { custom: VisitorbookContactPersonsField, span: 2 },
          { name: "comment", span: 3, rows: 4 },
        ],
      },
      {
        id: "visits",
        titleKey: "orga.visitorbook.visits",
        // Only for a stored visitor: the visits need its id.
        visible: ({ data }) => data?.id != null,
        render: ({ id }) =>
          id == null ? null : <VisitorbookVisitsSection id={id} />,
      },
    ],
  },
});
