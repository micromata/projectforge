# Keycloak Setup for ProjectForge

## Keycloak Admin: Realm Setup

- Create realm (e.g. `micromata.de`)
- Enable locales globally for the realm: `de`, `en`
- Create client `projectforge` (confidential, service account enabled)
- Note down client secret

## Keycloak Admin: User Attributes

Create the following custom user attributes with validators:

| Attribute      | Min Length | Max Length | Notes                                        |
|----------------|-----------|-----------|----------------------------------------------|
| `jiraUsername` | 0         | 100       |                                              |
| `mobilePhone`  | 0         | 255       |                                              |
| `gender`       | 0         | 100       | Enum: `MALE`, `FEMALE`, `DIVERSE`, `UNKNOWN` |
| `locale`       | 0         | —         | Validated automatically (`de`, `en`)         |
| `organization` | 0         | 255       |                                              |
| `description`  | 0         | 255       |                                              |
| `nickname`     | 0         | 255       |                                              |

## Keycloak Admin: Username Validator

- Min length: **2** (Keycloak default is 3; reduced for short usernames like `jo`)
- Max length: 255

## Keycloak Admin: User Permissions

- Users must **not** be allowed to edit their own attributes (name, first name, etc.)
- Exception: `locale` and mobile phone may be editable by the user later

## Keycloak Admin: Duplicate E-Mail Addresses

- Remove duplicate email addresses from inactive/deactivated users (e.g. replaced with `devnull@micromata.de`)
- When deactivating a user: either delete the email or leave it unchanged — do not reuse it

## ProjectForge: `projectforge.properties`

```properties
projectforge.login.handlerClass=KeycloakMasterLoginHandler

projectforge.keycloak.serverUrl=https://auth1.micromata.de
projectforge.keycloak.realm=micromata.de
projectforge.keycloak.clientId=projectforge
projectforge.keycloak.clientSecret=<secret>

# Optional: sync hashed passwords to Keycloak on PF login
# projectforge.keycloak.syncPasswords=true

# User attribute mappings: pfFieldName=keycloakAttributeName
projectforge.keycloak.userAttributes.jiraUsername=jiraUsername
projectforge.keycloak.userAttributes.mobilePhone=mobilePhone
projectforge.keycloak.userAttributes.gender=gender
projectforge.keycloak.userAttributes.locale=locale
projectforge.keycloak.userAttributes.organization=organization
projectforge.keycloak.userAttributes.description=description
projectforge.keycloak.userAttributes.nickname=nickname

# Group attribute mappings: pfFieldName=keycloakAttributeName
# Supported fields: description, organization
projectforge.keycloak.groupAttributes.description=description
```

## Keycloak Admin: Group Attributes

Group attributes are written automatically per group during sync — no global pre-configuration needed.

| Attribute     | Max Length | Notes                    |
|---------------|-----------|--------------------------|
| `description` | 1000      |                          |

## Sync Behavior

- User and group changes in PF are automatically synced to Keycloak (Phase 1)
- `jiraUsername`: if not set in PF, falls back to `username` (same behavior as LDAP)
- `locale`: stored as lowercase BCP 47 tag (`de`, `en`); set per user by PF
- Group attributes (e.g. `description`) do not need to be pre-configured in Keycloak — they are written automatically per group during sync
- Password sync: only when `syncPasswords=true`; passwords are transmitted correctly but users need appropriate login roles in Keycloak to sign in directly

## Keycloak Direct Login URL

Users can log in directly at:

```
https://<keycloak-host>/realms/<realm>/account
```

Example: `https://auth1.micromata.de/realms/micromata.de/account`

## Notes

- Phase 1 is functional: user/group sync to Keycloak works
- Not yet released to production PF
