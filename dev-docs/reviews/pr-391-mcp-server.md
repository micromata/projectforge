# Review: PR #391 – Remote MCP server (read only)

Status: **open** – remove this document once the points are resolved or the PR is merged.

PR: https://github.com/micromata/projectforge/pull/391 (`feature/remote-mcp-server`)

## Summary

The PR adds an opt-in, read-only MCP server at `/mcp` with a small OAuth authorization server of its own
(`/mcp/oauth/*`: dynamic client registration, PKCE S256, consent page in the existing ProjectForge session,
tokens bound to that session). Six datasets (projects, customers, tasks, timesheets, addresses, invoices) are
read through the existing `*EntityRest` classes (`getResultList`, `createListRow`, `getItem`) and attachments
through `AttachmentsService`.

The design is defensive throughout:
- the granted scopes are intersected with the user's current rights on every request
- the 2FA freshness is checked per entity path (`My2FARequestHandler`)
- a replayed authorization code revokes its grant
- refresh tokens rotate, and the reuse of an old one revokes the grant
- the endpoint checks the `Origin` header
- tokens and codes are kept only as hashes
- private address data is filtered out
- the tests show both halves of the access rules

## Findings (most severe first)

### 1. The global CSP probably blocks the consent redirect (verify)

`McpSessionOAuth.authorize` sets its own Content Security Policy via `ResponseEntity.header(...)`. But
`SecurityHeaderFilter` (mapped to `/*`) has already set the global policy
(`projectforge.security.csp-header-value`, containing `form-action 'self'`). Spring writes `ResponseEntity`
headers with `addHeader`, so both policies are sent and the browser enforces both.

The consent form posts to `/mcp/oauth/consent`, which answers with a 302 to `http://127.0.0.1:<port>`.
Chrome and Firefox apply `form-action` to redirects as well, so the global policy blocks the callback to the
MCP client. The same applies to `style-src`, which the global policy narrows.

There is no test of the CSP headers of the consent page.

Fix: replace the global header instead of adding a second one. Either call `response.setHeader(...)` on the
`HttpServletResponse` instead of using `ResponseEntity.header(...)`, or exclude the path in
`SecurityHeaderFilter`. Add a test that asserts exactly one CSP header, and check the end-to-end flow
(`claude mcp add …` / `claude mcp login …`) in a real browser with the default configuration.

### 2. Full-text search reveals the hidden address fields

`McpDatasets.hidden` removes the private address fields (`private*`, `birthday`) from results and filters.
The `query` argument, however, still searches the whole lucene index of `AddressDO`, private fields
included. So an agent can test whether a private street, phone number or birthday belongs to a person:
searching for the value returns the address.

Fix: refuse full-text search for `addresses`, or limit it to the business fields. At the least, document it
in the admin guide.

### 3. KDoc separated from its function in `OutgoingInvoicePdfTest`

The new test was inserted between the existing KDoc `/** A minimal but real PDF … */` and `fun pdf(...)`.
That comment now sits in front of the new test's KDoc and no longer documents `pdf()`. Move the test above
the old comment.

### 4. Consent page and scope labels hard-coded in German

`scopeLabels` and all texts of `authorize()` are hard-coded in German instead of coming from
`I18nResources`. The PR documents this as a limit of the prototype.

A later direction: a hand-built next page (e.g. `/next/mcp/consent`) with i18n and the normal layout,
posting to a JSON endpoint. It would also solve finding 1 and remove the html document built in a
controller.

### 5. Agent access ends at the browser session's idle timeout

MCP calls use the stored `HttpSession` but don't touch it (`lastAccessedTime`). So after the session's idle
timeout the agent gets `invalid_grant`, even within the one hour of the grant. This is consistent with
"the access ends with the browser session", but the admin guide reads as if one hour were the rule. Say
this more precisely in the admin guide (EN and DE).

### 6. Smaller points

- `available()` calls `RegisterUser4Thread.unregister()` in `finally`. A user who was registered for the
  thread before is removed as well. This is harmless today, since no filter registers a user for
  `/mcp/oauth`, but fragile.
- An empty or corrupt registry file stops the application from starting: `"".split("\n")` gives `[""]`,
  `require` fails and the bean can't be created. Better: log the bad entry and skip it.
- The limits `codes.size >= 1000` and `MAX_GRANTS` are global: one logged-in user can block the consents
  of all others for 60 s. A per-user limit for pending codes would be cheap.
- A JSON-RPC request with `"id": null` is accepted, although the spec doesn't allow a null id.
- If only `projectforge.profile.read` is granted, `tools/list` sends `enum: []` for the entity, which is an
  invalid JSON schema for some clients. Better to leave out the entity tools in that case.
- Strings longer than 8000 characters are cut without notice. The agent should get a marker
  (e.g. `…[truncated]`).
- Text attachments are always decoded as UTF-8; the charset of the media type is ignored.
- `@param:Value` on `resource` but plain `@Value` on `registryFile` (Kotlin 2 warns about the latter).
- `menu-url.test.ts`: double blank line before the new `describe`.
- The generated files (`i18nKeys.json`, `messages/generated.*.json` with the emoji keys of another branch,
  `lib/generated/changelog*.json`) are noise from a `gen` run against a newer develop. Fine, but best
  regenerated after the merge.

## Positive

- Follows the security rules of `CLAUDE.md`:
  - access is checked per endpoint by the rest classes and DAOs, with `@AccessChecked`
  - the consent is a POST with a CSRF token and a same-site check
  - the html is built with `HtmlElement`, so every value is escaped
  - the user and owner come from the session, never from the request
  - the tests show that `TEST_USER` is refused
- The changelog entry is in `changelog/unreleased/` (EN and DE). The admin guide has the section in both
  languages with the same anchor `[[mcp_server]]`.
- Making `createListRow` public is needed (it is called from another module) and documented.
