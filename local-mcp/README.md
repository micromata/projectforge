# ProjectForge MCP: reuse the existing browser session

The default demo now uses **ProjectForge itself** for OAuth authorization. Claude Code opens a
ProjectForge consent page. The page reads the existing ProjectForge session, shows the current user
and offers only requested read scopes allowed by that user's current rights and 2FA state.
It has **no password field, no username selector and no OTP field**.

## Start / stop

```sh
./local-mcp/start.sh             # Build and start; JDK 21 and Node 24+ required
./local-mcp/start.sh --no-build  # Reuse the existing built JAR
./local-mcp/stop.sh
```

The application binds only to `127.0.0.1:18080`. The real ProjectForge application runs with HSQLDB and
its generated test data. Runtime state, logs, passwords and caches are excluded from Git under `.local/`.
The running JAR is copied into `.local/mcp` so rebuilding does not replace the JVM's open archive.
Mail delivery, gateway push and Sipgate are disabled. No production data/configuration is copied.

Open http://localhost:18080 and log in normally, if there is no existing session:

- Web username: `admin`
- Web password: `Local-Demo-2026!`

Additional development users and their generated web passwords are in
`.local/mcp/home/testAccounts.txt`.

## Claude Code

The token-free `.mcp.json` registers `projectforge-local` at `http://localhost:18080/mcp`.
Run from this repository:

```sh
claude mcp login projectforge-local
```

Use the **same browser profile** in which you are already logged into this ProjectForge instance.
You should see only the recognized user, scope checkboxes, “Lesezugriff erlauben” and “Abbrechen”.
Claude Code receives its own scoped OAuth access automatically; it never receives the browser cookie.
You can also use `/mcp` → `projectforge-local` → Authenticate inside Claude Code.

OAuth client registrations allowed by a user persist across server restarts. Tokens and browser
sessions deliberately do not.
Native clients registered with a loopback IP (`127.0.0.1` or `[::1]`) may choose a different callback
port for each authorization. All other URI components must match; the token exchange must use the
exact callback chosen for that authorization. `localhost` callbacks still require an exact match.

Example prompt: `Zeige mir fünf Projekte aus ProjectForge und meinen angemeldeten Benutzer.`

**No existing session / expired session:** the browser uses ProjectForge's ordinary login and returns
to consent. **Existing valid session:** there is no additional login. Existing ProjectForge 2FA rules
are respected; the extension does not invent or bypass a second factor. Data scopes requiring an
expired second factor are unavailable until the normal ProjectForge verification has been completed.

## Scope and permission model

| Scope | Access |
| --- | --- |
| `projectforge.profile.read` | Current identity |
| `projectforge.projects.read` | Projects |
| `projectforge.customers.read` | Customers |
| `projectforge.tasks.read` | Tasks |
| `projectforge.timesheets.read` | Timesheets |
| `projectforge.addresses.read` | Addresses |
| `projectforge.invoices.read` | Invoice id, number and status |

The consent offers **requested ∩ currently permitted** scopes. The user may uncheck scopes.
The server validates selections again at submission. Tokens carry only selected scopes. MCP checks
current rights and 2FA again; a scope does not override ProjectForge's per-record DAO permission checks.
Search/list and detail reads also respect their mapped REST 2FA paths.
Missing scopes return HTTP 403 with an OAuth `insufficient_scope` challenge, including the required
scope, existing scopes and resource metadata so the client can request another consent.

Tools: `projectforge_whoami`, `projectforge_search`, `projectforge_get`. No write tools, raw SQL or
arbitrary entity paths. Output is allowlisted scalar fields. Search returns at most 50 records per
call, within a 1000-record window. Narrow the query for larger datasets.

This prototype deliberately **binds grants to the authorizing ProjectForge session**. Logout,
invalidated session, disabled account and relevant permission changes take effect on subsequent
requests. Access tokens last five minutes; rotating refresh grants last at most one hour. Refresh
does not create a new authentication or reset MFA freshness. Tokens are opaque random values; only
hashes are held in server memory. Restart requires authorization again.

Each grant holds one access and one refresh token; refreshing ends the previous access token. A reused
authorization code or outdated refresh token revokes the whole grant. A user keeps at most 10 grants; a
new one replaces the oldest. Client registration is unauthenticated, so it is limited to 10 per remote
address in 10 minutes (behind a reverse proxy, the real client address must be forwarded, e.g.
`server.forward-headers-strategy`). Registrations stay in a bounded in-memory pool until a logged-in
user allows the client; only allowed clients are kept and persisted.

## Code and verification

- `McpSessionOAuth.kt`: native-client registration, session-based consent, CSRF, PKCE code exchange,
  token rotation/revocation and scope calculation.
- `McpController.kt`: stateless Streamable HTTP MCP and access-checked data reads. It accepts only
  access tokens of `McpSessionOAuth`.
- Audit log (`org.projectforge.mcp`): registrations, consent decisions, issued, replaced and revoked
  grants, replayed codes or refresh tokens and reads refused by permissions. Users are logged by id,
  clients by their public client id; tokens, codes and client names are never logged.
- `projectforge-next/lib/menu-url.ts`: correct full-page return to the server authorization endpoint
  after an ordinary web login.

```sh
node local-mcp/session-smoke-test.mjs
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
GRADLE_USER_HOME="$PWD/.local/gradle" ./gradlew :projectforge-application:test \
  --tests 'org.projectforge.mcp.*' \
  --tests 'org.projectforge.rest.RestEndpointAccessCheckTest' --max-workers=2
(cd projectforge-next && node node_modules/vitest/vitest.mjs run lib/menu-url.test.ts)
```

Integration tests cover actual existing PF sessions, no second login fields, scope filtering,
unchecked scope denial even for a privileged user, CSRF, PKCE, replay, scope escalation, real database
reads, token refresh, and logout invalidation. JVM tests additionally check permission/MFA changes,
HTML escaping, token rotation and replay, the grant and registration limits and the bearer checks of
the MCP endpoint.
The integration test uses a separate grant for authorization-code replay and verifies that both its
access and refresh token are revoked while the ordinary read grant remains valid. Protocol tests cover
invalid version headers (HTTP 400), supported versions and initialization without a version header.

## Running on a real instance

This is a **local prototype**, not a deployed connector. To use it on a real ProjectForge instance:

- Deploy the backend changes and the login-return fix in `projectforge-next/lib/menu-url.ts`.
- Set `projectforge.mcp.enabled=true` and `projectforge.mcp.resource` to the HTTPS URL of `/mcp`.
  Disable `projectforge.mcp.local-demo` and the development mode.
- If an SSO reverse proxy protects ProjectForge, the browser authorization and consent routes
  (`/mcp/oauth/authorize`, `/mcp/oauth/consent`) stay behind it. The machine-facing routes (`/mcp`, the
  `.well-known` discovery documents, `/mcp/oauth/register`, `/token` and `/revoke`) must reach
  ProjectForge directly instead of the proxy's HTML login. Use a reviewed, narrow route configuration;
  **do not unprotect ProjectForge as a whole**. The MCP data routes still require their own bearer token,
  and the consent requires the real ProjectForge session and CSRF token.
- Forward the real client address (e.g. `server.forward-headers-strategy`), otherwise the registration
  limit applies to the proxy's address and therefore to all users together.

Before production: review per-dataset rights and field exposure, all deployment-specific 2FA paths,
session lifetime and multi-node storage. Grants are kept in memory on one server and bound to its
session objects; only native HTTP-loopback callbacks are permitted.
