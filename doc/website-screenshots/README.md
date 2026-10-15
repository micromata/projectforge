# Website screenshots

Scripts for the screenshots of www.projectforge.org (`site/uploads/screenshots/2026-*.png`). They fill a dev
instance with fictional sample data (company "Example Consulting", customers such as "Alpine Insurance AG",
user "Laura Becker") and take the pictures with Playwright. No real customer or personal data is used.

The data was designed for 2026-10-13; the scripts shift all dates relative to today, so the pictures stay current.

## Prerequisites

- `projectforge-next` with installed node modules (`npm install`), for Playwright. Another checkout works too:
  `PW_PACKAGE=/path/to/projectforge-next/package.json`.
- A free dev slot, here 5 (`:8085`). Other slots: `PF_URL=http://localhost:808n`.

## Steps

1. Fresh instance with test data:
   ```
   bin/pfDev.sh setup 5
   bin/pfDev.sh run 5
   ```
2. Seed with the full-access E2E account (the screenshot user doesn't exist yet), credentials from
   `~/ProjectForge-5/testAccounts.txt`:
   ```
   cd doc/website-screenshots
   PF_USER=e2e-full-access PF_PASSWORD=… node run-script.mjs seed/01-users-customers-projects.kts
   ```
   From now on the default user `laura` / `Screenshots-2026!` is used:
   ```
   node run-script.mjs seed/02-plugins.kts
   node run-script.mjs seed/03-vacations-timesheets.kts      # some minutes: ~700 time sheets
   node run-script.mjs seed/04-orders-invoices.kts
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
4. Start it again (`bin/pfDev.sh run 5`). The restart activates the plugin changes of step 2 and runs the plugin
   migrations, which a fresh database gets only on its second start (needed by the liquidity entries). Then:
   ```
   node run-script.mjs seed/05-cleanup-calendar-liquidity.kts
   node run-script.mjs seed/06-task-budgets.kts
   node run-script.mjs seed/07-calendar-leave-days.kts
   node run-script.mjs seed/08-datatransfer-areas.kts
   node run-script.mjs seed/09-remove-old-invoices.kts
   ./create-files.sh && node upload-files.mjs
   ```
5. Take the pictures and copy them, resized, into the website:
   ```
   node shoot.mjs /tmp/pf-screenshots
   for f in /tmp/pf-screenshots/*.png; do sips -Z 1920 "$f" --out ../../site/uploads/screenshots/; done
   ```
   Single scenes: `node shoot.mjs /tmp/pf-screenshots 2026-Calendar,2026-Statistics`. Check every picture before
   copying it.

The seed scripts are not idempotent: run them once on a fresh instance (`bin/pfDev.sh reset 5` starts over).

## Scenes

`scenes.mjs` defines name, page and interactions per picture. The calendar scene replaces the raw i18n key
"calendar.day" of the day view button by "Day" (a frontend bug, remove the workaround once it is fixed).
Leave applications and two-factor setup are not part of the scenes: these pages still use the legacy React UI.
