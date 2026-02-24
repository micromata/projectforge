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
- No Exceptions in phase 1 and 2.

## ProjectForge Admin: Duplicate E-Mail Addresses

- Remove duplicate email addresses from inactive/deactivated users (e.g. replaced with `devnull@micromata.de`)
- When deactivating a user: either delete the email or leave it unchanged — do not reuse it

## Migration Phases

### Phase 1 — PF as Master (ramp-up)

**Handler:** `KeycloakMasterLoginHandler`

- PF is authoritative; all user/group data is pushed from PF → Keycloak on every cache refresh
- PF → LDAP sync continues in parallel (via `LdapMasterLoginHandler` delegation)
- Passwords are **not** synced automatically; users cannot yet log in directly at Keycloak

```properties
projectforge.login.handlerClass=KeycloakMasterLoginHandler

projectforge.keycloak.serverUrl=https://auth1.micromata.de
projectforge.keycloak.realm=micromata.de
projectforge.keycloak.clientId=projectforge
projectforge.keycloak.clientSecret=<secret>

# User attribute mappings: pfFieldName=keycloakAttributeName
projectforge.keycloak.userAttributes.jiraUsername=jiraUsername
projectforge.keycloak.userAttributes.mobilePhone=mobilePhone
projectforge.keycloak.userAttributes.gender=gender
projectforge.keycloak.userAttributes.locale=locale
projectforge.keycloak.userAttributes.organization=organization
projectforge.keycloak.userAttributes.description=description
projectforge.keycloak.userAttributes.nickname=nickname

# Group attribute mappings: pfFieldName=keycloakAttributeName
projectforge.keycloak.groupAttributes.description=description
projectforge.keycloak.groupAttributes.organization=organization
```

### Phase 2 — PF as Master + Password Sync

**Handler:** `KeycloakMasterLoginHandler` (unchanged)

- Same as Phase 1, plus: on every successful PF login the user's password is pushed to Keycloak
- Users can then log in directly at Keycloak
- Tracked per user via `PFUserDO.lastKeycloakPasswordSync`

Additional property:

```properties
projectforge.keycloak.syncPasswords=true
```

### Phase 3 — Keycloak as Master (target state)

**Handler:** `KeycloakLoginHandler`

- Keycloak becomes authoritative; user/group data is pulled from Keycloak → PF on every cache refresh
- PF → LDAP sync continues via `LdapMasterLoginHandler` delegation
- Passwords are managed entirely in Keycloak; PF no longer stores or syncs passwords

```properties
projectforge.login.handlerClass=KeycloakLoginHandler

projectforge.keycloak.serverUrl=https://auth1.micromata.de
projectforge.keycloak.realm=micromata.de
projectforge.keycloak.clientId=projectforge
projectforge.keycloak.clientSecret=<secret>

# User attribute mappings: pfFieldName=keycloakAttributeName
projectforge.keycloak.userAttributes.jiraUsername=jiraUsername
projectforge.keycloak.userAttributes.mobilePhone=mobilePhone
projectforge.keycloak.userAttributes.gender=gender
projectforge.keycloak.userAttributes.locale=locale
projectforge.keycloak.userAttributes.organization=organization
projectforge.keycloak.userAttributes.description=description
projectforge.keycloak.userAttributes.nickname=nickname

# Group attribute mappings: pfFieldName=keycloakAttributeName
projectforge.keycloak.groupAttributes.description=description
projectforge.keycloak.groupAttributes.organization=organization
```

## Keycloak Admin: Group Attributes

Group attributes are written automatically per group during sync — no global pre-configuration needed.

| Attribute      | Max Length | Notes |
|----------------|-----------|-------|
| `description`  | 1000      |       |
| `organization` | 100       |       |

## Sync Behavior

- `jiraUsername`: if not set in PF, falls back to `username` (same behavior as LDAP)
- `locale`: stored as lowercase BCP 47 tag (`de`, `en`); set per user by PF
- Group attributes do not need to be pre-configured in Keycloak — written automatically per group

## Keycloak Direct Login URL

Users can log in directly at:

```
https://<keycloak-host>/realms/<realm>/account
```

Example: `https://auth1.micromata.de/realms/micromata.de/account`

## Status

- Phase 1 functional: user/group sync PF → Keycloak works
- Not yet released to production PF
