# Website screenshots

Scripts for the screenshots of www.projectforge.org (`site/uploads/screenshots/2026-*.png`). The pictures show
the fictional sample data of a test system (company "Example Consulting", customers such as "Alpine Insurance
AG", user "Laura Becker"), taken with Playwright. No real customer or personal data is used.

The sample data is created by the setup option "with test data" (`TestDataSeeder` in projectforge-business). It
was designed for 2026-10-13; all dates are shifted relative to the day of the setup, so the pictures stay current.

## Prerequisites

- `projectforge-next` with installed node modules (`npm install`), for Playwright. Another checkout works too:
  `PW_PACKAGE=/path/to/projectforge-next/package.json`.
- A free dev slot, here 5 (`:8085`). Other slots: `PF_URL=http://localhost:808n`.

## Steps

1. Fresh instance with test data. The sample users get the admin password; the scripts log in as
   `laura` / `Screenshots-2026!` (other credentials: `PF_USER`, `PF_PASSWORD`):
   ```
   bin/pfDev.sh setup 5 'Screenshots-2026!'
   bin/pfDev.sh run 5
   ```
2. Hide the plugins the pictures don't show (memo, to-do, ...); the change is applied on the next start:
   ```
   cd dev-docs/website-screenshots
   node run-script.mjs menu-plugins.kts
   ```
3. Stop the instance (`bin/pfDev.sh stop 5`) and prepare its home for the pictures:
   - append to `~/ProjectForge-5/projectforge.properties`:
     ```
     projectforge.development.mode=false
     projectforge.logoFile=
     projectforge.logoFileDark=
     projectforge.support.mail=support@example.org
     mail.session.pfmailsession.emailEnabled=true
     mail.session.pfmailsession.smtp.host=localhost
     mail.session.pfmailsession.smtp.port=2525
     ```
     (no development badge, no company logo; this deactivates the E2E accounts, `laura` is not affected. The mail
     settings activate the error digest job of the system dashboard; nothing listens on port 2525, so no mail is sent.
     A later `emailEnabled=false` in the file wins, remove it.)
   - move customer specific texts away, if the home was copied from a productive one:
     `mv ~/ProjectForge-5/resources/CustomerI18nResources*.properties ~/ProjectForge-5/resources-disabled/`
4. Start it again (`bin/pfDev.sh run 5`) and upload the sample files into the data transfer area:
   ```
   ./create-files.sh && node upload-files.mjs
   ```
   and create the sample chat and the sample problems of the system dashboard (backdated hourly counts, so that
   the tiles show trend bars; run it again shortly before the pictures, its counts are relative to the current hour):
   ```
   node chat-seed.mjs && node run-script.mjs dashboard-problems.kts
   ```
   If the home was copied from one with Lanes & Planes, append `projectforge.lanesandplanes.enabled=false` (tile
   without runs).
5. Take the pictures and copy them, resized, into the website:
   ```
   node shoot.mjs /tmp/pf-screenshots
   for f in /tmp/pf-screenshots/*.png; do sips -Z 1920 "$f" --out ../../site/uploads/screenshots/; done
   ```
   Single scenes: `node shoot.mjs /tmp/pf-screenshots 2026-Calendar,2026-Statistics`. Check every picture before
   copying it.

To start over with fresh data: `bin/pfDev.sh reset 5`, then step 1.

## Scenes

`scenes.mjs` defines name, page and interactions per picture. The calendar scene replaces the raw i18n key
"calendar.day" of the day view button by "Day" (a frontend bug, remove the workaround once it is fixed).
The gallery of leave and availability (`2026-Vacation*`, `2026-Availability*`, `2026-TeamVacationCalendar`) is
taken by `projectforge-next/e2e/tools/site-screenshots.mjs`, with the same viewport and scaling, in English and
German (`-de`). So are the chat pictures of the website and the user guide (`2026-Chat*`, sample chat of
`chat-seed.mjs`); the two of a confidential conversation (`ChatSecretOffer`, `ChatSecretDialog`) log in `max` as
the other side and end the conversation afterwards:
```
cd projectforge-next
PF_PASS='Screenshots-2026!' PF_BASE=http://localhost:8085/next node e2e/tools/site-screenshots.mjs \
  ChatList ChatRoom ChatThread ChatSecretOffer ChatSecretDialog
```
