# Identity Provider (IdP) Integration for ProjectForge

This module provides a provider-neutral integration layer for external identity providers.
Currently supported: **Keycloak** and **Authentik**.

The module uses a strategy pattern: shared login handlers (`IdpLoginHandler`, `IdpMasterLoginHandler`)
call an `IdpAdminClient` interface. The active provider is selected via `projectforge.idp.provider`.

## Quick Start

### Keycloak

```properties
projectforge.idp.provider=keycloak
projectforge.login.handlerClass=IdpMasterLoginHandler

projectforge.keycloak.serverUrl=https://auth.acme.com
projectforge.keycloak.realm=acme.com
projectforge.keycloak.clientId=projectforge
projectforge.keycloak.clientSecret=<secret>

# Attribute mappings (shared config)
projectforge.idp.userAttributes.jiraUsername=jiraUsername
projectforge.idp.userAttributes.mobilePhone=mobilePhone
projectforge.idp.userAttributes.gender=gender
projectforge.idp.userAttributes.locale=locale
projectforge.idp.userAttributes.organization=organization
projectforge.idp.userAttributes.description=description
projectforge.idp.userAttributes.nickname=nickname

projectforge.idp.groupAttributes.description=description
projectforge.idp.groupAttributes.organization=organization
```

### Authentik

```properties
projectforge.idp.provider=authentik
projectforge.login.handlerClass=IdpMasterLoginHandler

projectforge.authentik.serverUrl=https://authentik.acme.com
projectforge.authentik.apiToken=<api-token>

# Same attribute mappings as above
projectforge.idp.userAttributes.jiraUsername=jiraUsername
# ...
```

## Keycloak Setup

### Realm Setup
- Create realm (e.g. `acme.com`)
- Enable locales globally: `de`, `en`
- Create client `projectforge` (confidential, service account enabled)
- Note down client secret

### User Attributes

Create the following custom user attributes with validators:

| Attribute        | Min Length | Max Length | Notes                                           |
|------------------|------------|------------|-------------------------------------------------|
| `jiraUsername`   | 0          | 100        | length                                          |
| `mobilePhone`    | 0          | 255        | length                                          |
| `gender`         |            |            | options: `MALE`, `FEMALE`, `DIVERSE`, `UNKNOWN` |
| `locale`         | 0          | —          | Validated automatically (`de`, `en`)            |
| `organization`   | 0          | 255        | length                                          |
| `description`    | 0          | 255        | length                                          |
| `nickname`       | 0          | 255        | length                                          |
| `sambaNTWlanPassword`| 0      | 255        | WLAN password (Samba NT hash)                   |

### Username Validator
- Min length: **2** (Keycloak default is 3; reduced for short usernames like `jo`)

### User Permissions
- Users must **not** be allowed to edit their own attributes (name, first name, etc.)

## Authentik Setup

### Application & Provider
- Create new role, e. g. `projectforge-sync` with the following access:
  - Group: add, change, delete, view
  - User: add, change, delete, view, add/remove user to group, 
- Create an sync-user in Admin with this role.
- Create an API token for the user `projectforge-sync`: Admin → Directory → Tokens and App passwords → Create

### User Attributes
Authentik stores custom attributes as a JSON object on users/groups. No pre-configuration needed.

## Migration Phases

### Phase 1 — PF as Master (ramp-up)

**Handler:** `IdpMasterLoginHandler`

- PF is authoritative; all user/group data is pushed from PF → IdP on every cache refresh
- PF → LDAP sync continues in parallel (via `LdapMasterLoginHandler` delegation)
- Passwords are **not** synced automatically

```properties
projectforge.login.handlerClass=IdpMasterLoginHandler
projectforge.idp.syncPasswords=false
```

### Phase 2 — PF as Master + Password Sync

**Handler:** `IdpMasterLoginHandler` (unchanged)

- Same as Phase 1, plus: on every successful PF login the user's password is pushed to the IdP
- Users can then log in directly at the IdP
- Tracked per user via `PFUserDO.lastIdpPasswordSync`

```properties
projectforge.idp.syncPasswords=true
```

#### WLAN Password Sync (optional)

```properties
projectforge.idp.wlanPasswordAttribute=sambaNTWlanPassword
```

### Phase 3 — IdP as Master (target state)

**Handler:** `IdpLoginHandler`

- IdP becomes authoritative; user/group data is pulled from IdP → PF on every cache refresh
- PF → LDAP sync continues via `LdapMasterLoginHandler` delegation

```properties
projectforge.login.handlerClass=IdpLoginHandler
```

## ProjectForge Admin: Duplicate E-Mail Addresses

- Remove duplicate email addresses from inactive/deactivated users
- When deactivating a user: either delete the email or leave it unchanged — do not reuse it

## Sync Behavior

- `jiraUsername`: if not set in PF, falls back to `username` (same behavior as LDAP)
- `locale`: stored as lowercase BCP 47 tag (`de`, `en`); set per user by PF
- Group attributes do not need to be pre-configured in the IdP — written automatically per group
- Authentik: group membership is stored on the group (`users` array), not as a separate relationship

## Status

- Phase 1 functional: user/group sync PF → Keycloak works
- Authentik support: implemented, not yet tested in production
