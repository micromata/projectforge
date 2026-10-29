# Digital business cards (QR code, vCard, Apple Wallet)

Status: in implementation (everything but the Wallet pass done).

## Goal

A user creates one or more digital business cards from an address of the address book. A card shows a QR code with
the contact data (vCard), optionally with a photo, and can be added to the Apple Wallet. The photo is not part of the
QR code (too large) but a URL on the public gateway. The gateway knows nothing about business cards: it only answers
the (signed) url of a contact image with the image, which the address push brought there anyway.

## Decisions

- Several cards per user, e.g. with and without mobile number.
- A card is created from an address of the address book. The address matching the user (name, company) is suggested;
  the user can pick another one.
- The user selects the fields to take over and may edit them by hand. A card keeps its **own copy** of the fields, so
  later changes of the address do not change a published card unnoticed.
- Apple Wallet first, without a native app (`.pkpass`). Google Wallet is not part of the first version.
- The Wallet certificate stays on the main instance, never on the gateway.

## Apple Wallet without a native app

Safari or Mail on the iPhone opens a `.pkpass` file (`application/vnd.apple.pkpass`) and offers "Add to Wallet".

- A `.pkpass` is a ZIP with `pass.json`, images, `manifest.json` (SHA-1 of each file) and `signature` (detached PKCS#7).
  Signing with BouncyCastle.
- Pass style `generic`, barcode `PKBarcodeFormatQR` with the vCard as message (`messageEncoding` `utf-8`).
- The photo is **embedded** in the bundle (`thumbnail.png`); Wallet does not load remote images. The photo URL
  (`PHOTO;VALUE=URI:<gatewayPublicUrl>/…`) only lives in the vCard of the QR code. Some scanners ignore it.
- Updating an issued pass would need a web service (`webServiceURL`, push): not in the first version.

### Certificate setup (once per installation)

A Pass Type ID certificate is separate from the iOS signing certificates, but belongs to the same Apple Developer team.
The macOS notarization of an existing app does not help: it needs a Developer ID certificate, a Pass Type ID certificate
has to be created anew in the Apple Developer portal.

1. developer.apple.com, Certificates, Identifiers & Profiles, Identifiers, Pass Type IDs: create e.g.
   `pass.<reverse-domain>.businesscard`.
2. In this Pass Type ID: *Create Certificate* with a CSR, download `pass.cer`.
3. Export certificate and private key as `.p12`.
4. Download the *Apple WWDR G4* intermediate certificate (apple.com/certificateauthority).
5. Configure path, password, team id, pass type id and organization name in `projectforge.properties`. Without them the
   Wallet button is not shown.

The certificate is valid for about one year: document the renewal in the admin guide.

## Existing building blocks

- vCard: `VCardUtils.buildVCardString`, `AddressDao.exportVCard(addressDO, embedImage)` (ez-vcard).
- QR code: `BarcodeServicesRest.getQrCodeDataUrl` (zxing).
- Photos: `AddressImageDO`, `AddressImageDao`.
- Gateway push: `GatewaySyncPushService` (delta, full sync, `retain`, `X-Gateway-Secret`); receiving side
  `GatewaySyncController`, `GatewaySyncService`, DTOs in `gateway/sync/dto/`. The address images already reach the
  gateway (`AddressImageDO`).
- Public gateway paths: `GatewayEndpointFilter` (allow-list) and `GatewaySecurityConfig` (permitAll, `/rsPublic/**`).
- Gap: there is no relation user to own address (`PFUserDO`, `EmployeeDO` have no address field). The suggestion is
  based on name, e-mail and company, see "Resolved questions".

## Design

### Data model (main instance)

No table. The cards are personal data only their owner works with (no queries across users, no history), so they are
kept as json in the user preferences (`UserPrefService`, area `businessCard`, name `cards`, value `BusinessCards`, a
list of `BusinessCard`). Access is per user by construction; the cards go with the user.

`BusinessCard`: `id` (uuid, assigned by the server, the uid of its vCard; no secret), card name, optional source
address id, copied fields (business: title, name, company, division, position, e-mail, phone, mobile, fax, web,
address, note; private: e-mail, phone, mobile, address, birthday), `includePhoto`. The editor ticks the business
fields that have a value when the data is taken over from an address, never the private ones; an unticked field is
stored empty, and ticking it again fills it from the source address. At
most 20 cards per user. `BusinessCardService` works only on the list of the logged-in user: a posted id only selects
among the own cards, so nobody reaches the cards of another user. The photo is never stored in the card.
The photo is only published from the user's **own** address (readable, not deleted, name or e-mail matches the user,
see `AddressUserMatcher`): otherwise anybody who may read an address could make the photo of a colleague public. The
fields may be taken over from any readable address, but then without photo. The service has no user parameter on
purpose, it always works on the logged-in user.

### Contact image on the gateway (stateless)

`GET <gatewayPublicUrl>/rsPublic/contactImage/{base64url(addressUid)}/{signature}`

- `ContactImageUrl` (business module) builds and checks the url. The signature is an HMAC-SHA256 of the address uid,
  with a key derived from `projectforge.security.authenticationTokenEncryptionKey`, which both instances share anyway
  (DAV tokens are pushed encrypted). Nobody gets an image by trying uids. The uid is base64url so that any uid works
  as a path segment.
- `GatewayContactImageController` checks the signature, looks up the address by uid and delivers its
  `AddressImageDO` with the fixed image content type. Anything else (bad signature, unknown or deleted address, no
  image) is a plain 404 without a hint. `GatewayEndpointFilter` allows the path (`/rsPublic/**` is permitAll already).
- No new table, no new sync endpoint: the address push already brings every address with its image
  (`pushAddressBooks`/`toAddressDtos`, only with `projectforge.gateway.push.syncAddresses`). The uid is the pushed one,
  `ContactImageUrl.addressUid` (shared with the push).
- Limit: a handed-out url stays valid as long as the address and its image exist. Switching off `includePhoto` does
  not revoke it, only deleting the image or changing the key does. Recipients import the photo into their address
  book anyway; mention this in the admin guide.
- Without a configured gateway (or key) there is no photo url, the card works without a photo.

### REST and UI (Next)

List "My business cards"; editor with address suggestion, field checkboxes, manual edit, live QR preview, vCard
download and "Add to Wallet". Endpoints carry `@AccessChecked`; the owner is always the logged-in user, never taken
from the request; state changes only with POST/PUT/DELETE. A plain REST controller (`BusinessCardRest`), no server-side UILayout.

### PassKit service

`PassKitService` builds and signs the `.pkpass` on the main instance (QR with the vCard, photo embedded as
`thumbnail.png`). The download is only there: the gateway does not serve passes.

## Steps

1. Done: `BusinessCard`/`BusinessCards` as json in the user preferences and `BusinessCardService` (id, limit, own cards only).
2. Done: address suggestion (`AddressUserMatcher`), vCard builder (`BusinessCardVCard`).
3. Done: `ContactImageUrl` and `GatewayContactImageController` (signed contact image url on the gateway).
4. Done: photo url in the vCard of a card (only with `includePhoto`, a gateway and an image of the own source address).
5. Done: `BusinessCardRest` (`/rs/myBusinessCards`) and the next page `myBusinessCards` (list with QR codes, editor
   with address suggestion, field checkboxes, live QR preview, vCard download). Reached from the user menu
   (`MenuItemDefId.MY_BUSINESS_CARDS`, added in `MenuRest` only, not in the main menu) and a button of the address list.
6. `PassKitService`, configuration, Wallet button.
7. Docs: admin guide EN and DE (certificate, photo url and its limit), extend `HOWTO-TEST-GATEWAY.md`, `limit_req` for
   `/rsPublic/contactImage/` in the nginx example. Done: changelog entries (`changelog/unreleased/20261029-*`), i18n.
8. Later: re-evaluate Google Wallet (Google Cloud account, signed JWT "Save to Wallet" links).

## Tests

- vCard with photo URL; `.pkpass` content (ZIP files, manifest hashes, signature verifiable with a test certificate
  via `openssl smime -verify`).
- Access: another user (and the admin) can neither read, change nor delete a card of the owner.
- Contact image url: signature valid only for its uid and key; the gateway answers 404 for bad signature, unknown or
  deleted address and missing image (`ContactImageUrlTest`, `GatewayContactImageControllerTest`).
- Manual with a local gateway (see `HOWTO-TEST-GATEWAY.md`): the photo url of the QR code delivers the image.
- Manual: scan the QR code with the iPhone camera; open the `.pkpass` in Safari and add it to the Wallet.
- e2e for the Next pages.

## Resolved questions

### Address suggestion: reuse of the birthday butler matching

The matching lives privately in `BirthdayButlerService.getBirthdayList`: an address belongs to a user if first name
and last name are equal (trimmed, ignoring case) to `PFUserDO.firstname`/`lastname`; the addresses are first filtered
by `organization` containing `projectforge.birthdaybutler.organization`. It cannot be called as it is (private,
works for all users of a month), but the rule is reused:

- Extract the name comparison into a small shared function (e.g. `AddressDao`/`AddressUserMatcher`) used by the butler
  and the business card, so both stay identical.
- Suggestion for the logged-in user, ranked: (1) name equal and e-mail equal to the user's e-mail (also alternative
  e-mails), (2) name equal and organization containing the butler organization, if configured, (3) name equal.
  Deleted, not up-to-date or inactive addresses are skipped. The best hit is preselected, all hits are offered, the
  user may search for another address.
- Only a suggestion, never stored as a user to address relation: the card keeps the chosen address id as a reference.

### Rate limiting of the public gateway endpoint

- Guessing is prevented by the signature (HMAC); invalid requests are answered with a plain 404 and cost almost
  nothing. No login protection is needed (there is no secret to try).
- `PasswordResetRateLimiter` is `internal` and specific (user/mail), so it is not reused. A limit for the download
  volume belongs to the reverse proxy: add `limit_req` for `/rsPublic/contactImage/` to the nginx example
  `docker/compose/gateway/nginx/nginx.conf` (it has none yet) and describe it in the admin guide.

### Pass fields and layout

Style `generic` (a contact is not a ticket or a coupon):

- Front: primary field = name; secondary = position; auxiliary = company; thumbnail = photo (if the card has one);
  logo + `logoText` = organization.
- Back: e-mail, phone, mobile (only if selected for the card), web, address, note; each with a `dataDetectorTypes`
  so that Wallet makes them tappable.
- Barcode: QR with the vCard (error correction `M`); `altText` = card name. A vCard with many fields makes a dense
  QR code: the editor shows the QR preview and warns when the message exceeds about 1000 characters.
- Colors: `backgroundColor`, `foregroundColor`, `labelColor` as properties with a neutral default (white background,
  dark text); logo from a property with the bundled ProjectForge icon as fallback. The installation sets its own logo
  and colors, there is no per user design in the first version.
- `serialNumber` = card id, `passTypeIdentifier`/`teamIdentifier` from the configuration, `organizationName` from
  the configuration. No `relevantDate`, no `expirationDate`.

## Open points

- Manual test with a real gateway and iPhone: does the address book of the scanning phone load the photo url of a
  QR-scanned vCard? (Untested; without it the card works, just without photo.)
- Exact property names of the Wallet and design configuration (decide with the implementation).
- Which size the photo should have on the gateway: the push brings the address image as stored; the vCard could point
  to it as it is (check typical sizes before deciding on a scaled variant).
