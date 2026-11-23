# Rechnung aus dem Auftrag anlegen

**Status:** Plan, nicht begonnen. Finance hat geantwortet, die Entscheidungen stehen unter [Entscheidungen von Finance](#entscheidungen-von-finance).

## Ausgangslage

Vom Auftrag führt kein Weg zur Rechnung. Wählt man in einer Rechnungsposition eine Auftragsposition
(`components/features/invoice/edit/order-position-field.tsx`), wird nur verknüpft, übernommen wird nichts. Wicket
hatte das auch nicht.

Mit der E-Rechnung kommt die Bestellnummer des Kunden (BT-13, `RechnungDO.customerOrderNumber`) dazu. Sie steht
oft schon am Auftrag als Kundenreferenz (`AuftragDO.referenz`) und muss heute in jede Rechnung von Hand übertragen
werden.

## Ziel

Im Auftrag gibt es einen Button „Rechnung anlegen“ für den ganzen Auftrag und je Position. Er öffnet eine neue,
ungespeicherte Rechnung, vorbelegt mit:

- Kunde, Projekt, Betreff (Titel des Auftrags);
- Bestellnummer = Kundenreferenz des Auftrags;
- den Positionen, jeweils verknüpft mit der Auftragsposition (Text, Menge 1, Betrag, Standard-USt, Leistungszeitraum);
- den E-Rechnungsdaten aus dem Konto (`OutgoingInvoiceEntityRest.fillEInvoiceFieldsFromAccount`). Ohne Konto
  bleibt das leer, das ist gewollt: Konten werden oft erst beim Schreiben der Rechnung angelegt.

### Betrag je Position (Vorschlag)

- Hat die Position fällige, noch nicht fakturierte Zahlplanraten (`PaymentScheduleInfo.toBeInvoiced`), kommt deren
  Summe.
- Sonst kommt der noch nicht fakturierte Rest (`OrderPositionInfo.notYetInvoiced`).
- Beim ganzen Auftrag fallen Positionen mit Betrag 0 weg. Bleibt keine übrig, kommen alle offenen Positionen mit
  leerem Betrag.
- Zahlplanraten ohne Position: Hat der Auftrag genau eine Position, werden sie ihr zugeschlagen. Sonst wird jede Rate
  eine eigene, unverknüpfte Position mit ihrem Kommentar als Text.

Das ist dieselbe Logik wie bei „zu fakturieren“ (`OrderInfo.calculateToBeInvoicedSum`).

## Umsetzung

Über URL-Parameter am neuen Eintrag, nach dem vorhandenen Muster `edit.newEntryParams`
(`lib/page-def/types.ts`, z. B. Task `?parentTaskId=`): `/invoice/new?order=<id>[&orderPosition=<posId>]`. Das
übersteht einen Reload, die URL ist teilbar, und die Links je Position kommen ohne Zusatzaufwand. Nicht über
`edit.convert` (wie beim Storno): davon gibt es nur eines je Seite, es postet ungespeicherte Formularwerte, und die
Vorbelegung geht beim Reload verloren.

### Backend

- **`OutgoingInvoiceEntityRest.newBaseDTO(request)`** baut die Rechnung, wenn `order` gesetzt ist.
  - Zugriff: `checkSingleInvoiceDetailAccess()`, den Auftrag über `AuftragDao.find` laden (prüft den Lesezugriff);
    das Insert-Recht prüft `newEntry` schon.
  - Danach `fillEInvoiceFieldsFromAccount`, wie in `prepareClone`.
- **Positionen** in einer `internal fun buildInvoicePositionsFromOrder(…)` im Companion, ohne Spring testbar wie
  `prepareInvoiceClone`. Die Daten kommen aus `AuftragsCache` (`getOrderInfo`, `getOrderPositionInfo`).
  - Kandidaten: nicht gelöscht, nicht `vollstaendigFakturiert`, nicht LOST.
  - Eine angefragte Position, die nicht zum Auftrag gehört, ergibt 406.
  - USt: `ConfigurationParam.FIBU_DEFAULT_VAT`, denn das Frontend setzt sie nur beim Hinzufügen einer Zeile.
  - Leistungszeitraum der Position nur, wenn er dort eigens gesetzt ist (`hasOwnPeriodOfPerformance()`).
  - Keine Kostenzuweisungen.
- **`dto/Auftrag.kt`**: Flag `invoiceInsertAccess` (Insert-Recht für Rechnungen und `hasSingleInvoiceDetailAccess`),
  gesetzt in `OrderEntityRest.transformFromDB` nach dem Muster von `invoicesSelectAccess`.

### Frontend

- `invoice.page.tsx`: `edit.newEntryParams: ["order", "orderPosition"]`.
- `order.page.tsx`: `crossLinks`-Eintrag „Rechnung anlegen“ (`prominent`). Ausgeblendet, wenn der Auftrag
  ungespeichert ist oder das Flag fehlt.
- Je Position: Link in `edit/position-invoices.tsx`, neben „fakturiert / noch nicht fakturiert“. Nur für gespeicherte,
  nicht vollständig fakturierte Positionen.
- Rücksprung zum Auftrag nach dem Speichern: `useEditReturn` kennt nur feste Routen, also erst einmal ohne.

### Sonstiges

- i18n: `fibu.auftrag.createInvoice` plus Tooltip, danach `bin/pfDev.sh gen`.
- Changelog: `changelog/unreleased/<datum>-invoice-from-order.json`.

## Tests

- `buildInvoicePositionsFromOrder` mit diesen Fällen:
  - Rest ohne Zahlplan;
  - fällige Raten;
  - nicht fällige Rate;
  - Rate ohne Position, bei einer bzw. mehreren Positionen;
  - vollständig fakturierte Position;
  - einzelne Position und fremde Position.
- Zugriff: `TEST_USER` wird abgelehnt, ein FIBU-Nutzer bekommt die Vorbelegung.
- Im Browser: Auftrag mit Zahlplan, ganzer Auftrag und einzelne Position; ohne Rechnungsrecht ist kein Button zu
  sehen.

## Entscheidungen von Finance

1. **Positionstext:** Titel der Auftragsposition. Bei einer Zahlplanrate kommen Position und Bemerkung der Rate in den Text.
2. **Einheit:** Bei der Menge ist eine Einheit wählbar (Stunden, Tage, Stück). Im Auftragsbuch kommt ein Tagessatz bzw.
   Stundensatz dazu, aus dem sich der Betrag berechnet.
3. **Betreff:** Titel des Auftrags.
4. **Zahlungsziel:** In den Stammdaten des Kontos hinterlegt (z. B. 90 Tage); die Rechnung übernimmt es von dort.
5. **Kostenzuweisungen:** werden vorbelegt.
6. **Bestellnummer:** Die Nummer der Position hat Vorrang. Fehlt sie, gilt die aus dem Reiter Rechnungsdaten des Auftrags.
7. **Zahlplan:** Die Position ist Pflichtfeld; es gilt die Bestellnummer der Position.
8. **Raten ohne Position** gibt es nicht, Raten und Positionen mischen sich also nicht. Die Sonderregel oben entfällt.

### Debitor

Das DATEV-Konto ist der Debitor, die Firma, die zahlt. Er kann vom Kunden abweichen (Konzern als Kunde, Rechnungen an
Töchter) und lässt sich daher nicht aus dem Kunden ableiten. Nur Finance hat Zugriff auf die Konten: Die Vorbelegung setzt
das Konto nur, wenn es eindeutig feststeht; ein Auswahlfeld dafür bekommen nur Nutzer mit Konto-Recht.

### Weiter als Annahme

Betragsregel wie oben (mit Satz: Menge × Satz), nicht fällige Raten entfallen, kein Rücksprung zum Auftrag nach dem Speichern.
