/** A business card as BusinessCard (Kotlin) serializes it; `id` unset for a new one. */
export interface BusinessCard {
  id?: string | null;
  cardName?: string | null;
  addressId?: number | null;
  includePhoto?: boolean;
  firstName?: string | null;
  lastName?: string | null;
  title?: string | null;
  organization?: string | null;
  positionText?: string | null;
  email?: string | null;
  businessPhone?: string | null;
  mobilePhone?: string | null;
  website?: string | null;
  street?: string | null;
  zipCode?: string | null;
  city?: string | null;
  country?: string | null;
  division?: string | null;
  fax?: string | null;
  privateEmail?: string | null;
  privatePhone?: string | null;
  privateMobilePhone?: string | null;
  privateStreet?: string | null;
  privateZipCode?: string | null;
  privateCity?: string | null;
  privateCountry?: string | null;
  /** ISO date (yyyy-MM-dd). */
  birthday?: string | null;
  comment?: string | null;
}

/**
 * The fields of a card the user picks and edits (all but name, address and photo), with the keys of their labels
 * spelled out: the i18n generator only exports keys it finds literally. The private ones are unticked when the
 * data is taken over from an address; the user ticks what the card shall show.
 */
export const BUSINESS_CARD_FIELDS = [
  { field: "title", labelKey: "businessCard.title", private: false },
  { field: "firstName", labelKey: "businessCard.firstName", private: false },
  { field: "lastName", labelKey: "businessCard.lastName", private: false },
  {
    field: "organization",
    labelKey: "businessCard.organization",
    private: false,
  },
  { field: "division", labelKey: "businessCard.division", private: false },
  {
    field: "positionText",
    labelKey: "businessCard.positionText",
    private: false,
  },
  { field: "email", labelKey: "businessCard.email", private: false },
  {
    field: "businessPhone",
    labelKey: "businessCard.businessPhone",
    private: false,
  },
  {
    field: "mobilePhone",
    labelKey: "businessCard.mobilePhone",
    private: false,
  },
  { field: "fax", labelKey: "businessCard.fax", private: false },
  { field: "website", labelKey: "businessCard.website", private: false },
  { field: "street", labelKey: "businessCard.street", private: false },
  { field: "zipCode", labelKey: "businessCard.zipCode", private: false },
  { field: "city", labelKey: "businessCard.city", private: false },
  { field: "country", labelKey: "businessCard.country", private: false },
  { field: "comment", labelKey: "businessCard.comment", private: false },
  {
    field: "privateEmail",
    labelKey: "businessCard.privateEmail",
    private: true,
  },
  {
    field: "privatePhone",
    labelKey: "businessCard.privatePhone",
    private: true,
  },
  {
    field: "privateMobilePhone",
    labelKey: "businessCard.privateMobilePhone",
    private: true,
  },
  {
    field: "privateStreet",
    labelKey: "businessCard.privateStreet",
    private: true,
  },
  {
    field: "privateZipCode",
    labelKey: "businessCard.privateZipCode",
    private: true,
  },
  { field: "privateCity", labelKey: "businessCard.privateCity", private: true },
  {
    field: "privateCountry",
    labelKey: "businessCard.privateCountry",
    private: true,
  },
  { field: "birthday", labelKey: "businessCard.birthday", private: true },
] as const;

export type BusinessCardField = (typeof BUSINESS_CARD_FIELDS)[number]["field"];

/** Per field whether it is on the card. */
export type IncludedFields = Record<BusinessCardField, boolean>;

export interface BusinessCardPreview {
  qrCodeUrl: string;
  vCardLength: number;
  withPhoto: boolean;
  photoAllowed: boolean;
}

export interface BusinessCardInfo {
  card: BusinessCard;
  preview: BusinessCardPreview;
}

export interface MyBusinessCardsInit {
  cards: BusinessCardInfo[];
  suggestion: BusinessCard | null;
  maxCards: number;
  gatewayConfigured: boolean;
}

/** From this length on the QR code gets dense (see BusinessCardRest.Preview.vCardLength). */
export const DENSE_VCARD_LENGTH = 1000;
