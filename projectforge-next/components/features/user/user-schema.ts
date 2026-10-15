import { z } from "zod";
import { PF_USER_METADATA } from "@/lib/metadata/pf-user.generated";
import { fromMetadata } from "@/lib/validation/from-metadata";
import { INTEGER } from "@/lib/validation/markers";

/**
 * Every rule of the user's own properties — maximum length, whether a field is mandatory — comes from
 * PFUserDO through `lib/metadata/pf-user.generated.ts`. Hand-written are the fields PFUserDO has no
 * property for: the groups, the rights, the LDAP values (an XML column of the entity, an object in the
 * DTO), the initial passwords and the read-only flags and texts of the request.
 */
const m = fromMetadata(PF_USER_METADATA);

const groupRef = z.looseObject({
  id: z.number(),
  displayName: z.string().optional(),
});

const userRight = z.looseObject({
  rightId: z.string(),
  value: z.string().nullable().optional(),
  booleanValue: z.boolean().nullable().optional(),
});

const wholeNumber = z
  .number()
  .nullable()
  .refine((v) => v == null || Number.isInteger(v), INTEGER);

const ldapValues = z.looseObject({
  uidNumber: wholeNumber,
  gidNumber: wholeNumber,
  homeDirectory: z.string().nullable(),
  loginShell: z.string().nullable(),
  sambaSIDNumber: wholeNumber,
  sambaPrimaryGroupSIDNumber: wholeNumber,
  sambaNTPassword: z.string().nullable(),
});

const readOnlyText = z.string().nullable();

/**
 * Which fields the form has mirrors org.projectforge.rest.dto.User. A save posts these values *as* the
 * DTO (there is no merge with what was loaded), so everything the backend needs back is carried even
 * where nothing renders it — `created`, the read-only flags.
 */
export const userSchema = z.object({
  // null while the user is new — Spring assigns the id on the first save.
  id: z.number().nullable(),
  username: m.requiredString("username"),
  firstname: m.nullableString("firstname"),
  nickname: m.nullableString("nickname"),
  lastname: m.nullableString("lastname"),
  gender: z.string().nullable(),
  organization: m.nullableString("organization"),
  email: m.nullableString("email"),
  alternativeEmails: m.nullableString("alternativeEmails"),
  formerEmails: m.nullableString("formerEmails"),
  mobilePhone: m.nullableString("mobilePhone"),
  jiraUsername: m.nullableString("jiraUsername"),
  description: m.nullableString("description"),
  hrPlanning: m.booleanField("hrPlanning"),
  deactivated: m.booleanField("deactivated"),
  restrictedUser: m.booleanField("restrictedUser"),
  localUser: m.booleanField("localUser"),
  timeZone: z.string().nullable(),
  locale: z.string().nullable(),
  dateFormat: m.nullableString("dateFormat"),
  excelDateFormat: m.nullableString("excelDateFormat"),
  timeNotation: z.string().nullable(),
  personalPhoneIdentifiers: m.nullableString("personalPhoneIdentifiers"),
  sshPublicKey: m.nullableString("sshPublicKey"),
  gpgPublicKey: m.nullableString("gpgPublicKey"),
  /** The groups; an empty list rather than null, which is what the picker holds. */
  assignedGroups: z.array(groupRef),
  /** The configurable rights, maintained by UserRightsField. */
  userRights: z.array(userRight),
  ldapValues,
  /** The optional initial passwords of a new user; their quality is the backend's check. */
  password: z.string().nullable(),
  wlanPassword: z.string().nullable(),
  // Shown read-only, sent back untouched.
  lastLoginFormatted: readOnlyText,
  lastPasswordChangeFormatted: readOnlyText,
  lastWlanPasswordChangeFormatted: readOnlyText,
  posixConfigured: z.boolean(),
  sambaConfigured: z.boolean(),
  externalUserManagementSystem: z.boolean(),
  wlanPasswordSupported: z.boolean(),
  phoneIdentifiersSupported: z.boolean(),
  created: z.string().nullable(),
});

export type UserValues = z.infer<typeof userSchema>;

/**
 * Field names of the form, so a server validation error can be checked against what actually renders
 * (see applyServerValidationErrors). The LDAP values are nested: their errors name the path
 * (`UserEntityRest.validatePosixValues`), which is what the fields are bound to.
 */
export const USER_FIELDS = [
  ...Object.keys(userSchema.shape).filter((name) => name !== "ldapValues"),
  "ldapValues.uidNumber",
  "ldapValues.gidNumber",
  "ldapValues.homeDirectory",
  "ldapValues.loginShell",
  "ldapValues.sambaSIDNumber",
  "ldapValues.sambaPrimaryGroupSIDNumber",
] as readonly string[];
