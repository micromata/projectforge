/** A user or group as the DTO carries it: the id is what `copyTo` stores. */
// A type alias, not an interface: only an alias is assignable to the schema's loose object.
export type AddressbookRef = {
  id: number;
  displayName?: string;
};

/** org.projectforge.rest.dto.Addressbook. */
export interface AddressbookDetail {
  id: number | null;
  title?: string;
  description?: string | null;
  owner?: AddressbookRef | null;
  fullAccessUsers?: AddressbookRef[];
  fullAccessGroups?: AddressbookRef[];
  readonlyAccessUsers?: AddressbookRef[];
  readonlyAccessGroups?: AddressbookRef[];
  created?: string | null;
  lastUpdate?: string | null;
}

/** Projection the list page renders — the same DTO, with the id the table keys rows by. */
export interface AddressbookListRow extends AddressbookDetail {
  id: number;
}
