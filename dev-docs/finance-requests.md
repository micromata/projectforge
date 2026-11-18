# Wünsche des Finance-Teams: Buchungskonten, Auftragsbuch, Rechnung auf Knopfdruck

**Status:** Plan, nicht begonnen. Die Rechnung aus dem Auftrag (Schritt 4) ist in
[invoice-from-order.md](invoice-from-order.md) ausgearbeitet und wartet auf die Antworten von Finance.

## Empfehlung: Konten sind Sache von Finance

Nutzer außerhalb von Finance sollen sich nicht mit Buchungskonten beschäftigen. Sie können sie auch gar nicht lesen
(kein Zugriff auf `KontoDO`). Deshalb gibt es am Auftrag **kein Feld „Konto“**:

- Das Konto kommt serverseitig über `kunde.konto` in die Rechnung (`OutgoingInvoiceEntityRest.fillEInvoiceFieldsFromAccount`).
- Entfallen sind damit die Pflichtauswahl beim Status „beauftragt“, die Option „nicht vorhanden, anlegen“ mit
  Mail an Finance und das Klappfeld mit den Kontodaten im Auftrag. Fehlt das Konto, legt Finance es beim Schreiben der
  Rechnung an.
- Der Button „aus Konto ergänzen“ im Auftrag holt die Kontaktdaten serverseitig. Nutzer sehen nur die übernommenen Werte,
  nie das Konto.

## Reihenfolge

| Schritt | Inhalt                                                                                              | Stand       |
|---------|-----------------------------------------------------------------------------------------------------|-------------|
| 0       | Diese Doku, offene Fragen an Finance (siehe invoice-from-order.md)                                  | Fragen offen |
| 1       | Kontoliste: Filter Debitoren / Kreditoren / Sachkonten (konfigurierte Nummernkreise, `AccountingConfig`) | umgesetzt   |
| 2       | Kontomaske: BT-58 unter BT-56, BT-Nummern an den Feldern, fehlende gängige BT-Felder (siehe unten)  | teils       |
| 3       | Auftrag: BT-12, BT-13 (`customerOrderNumber`, auch Position), BT-19, BT-56/58 mit Vorrang vor dem Konto, Leistungszeitraum (BT-73/74) im Zahlplan | umgesetzt (ohne Snapshots/Excel) |
| 4       | Rechnung auf Knopfdruck, Variante 1 (Gesamtauftrag, Position, Zahlplanposition)                      | offen       |
| 5       | Rechnungsassistent (Variante 2)                                                                      | später      |
| 6       | Kunden-Datenabgleichsblatt als PDF aus der Kontomaske                                                 | offen       |

## Bereits vorhanden

- `KontoDO`: Leitweg-ID, Adresse, USt-Id, E-Mail für E-Rechnung, Kontakt (BT-56), Abteilung, E-Mail (BT-58), Kostenstelle (BT-19).
- `RechnungDO`: Bestellnummer (BT-13), Kontakt, Kostenstelle, Leistungszeitraum (BT-73/74, auch je Position).
- Auftrag und Auftragsposition: Leistungszeitraum. Fehlt noch im Zahlplan (`PaymentScheduleDO`).

## Festlegungen

- BT-13 am Auftrag ist ein eigenes Feld; die Kundenreferenz (`referenz`) bleibt unverändert (sie dient auch für Jira-Links).
- Nach dem Anlegen entscheidet Finance in der Rechnungsmaske, ob die Rechnung „geplant“ oder direkt „gestellt“ wird.

## Offen zu Schritt 2

BT-58 steht nun unter BT-56 und die Felder des Kontos nennen ihre BT-Nummer. Das Konto trägt damit alle Felder der
Rechnung, die aus ihm vorbelegt werden. Welche weiteren „gängigen“ BT-Felder Finance am Konto pflegen will (z. B.
Zahlungsbedingungen, Käufer-Referenz), ist offen und kommt mit den Fragen an Finance.

## Zu Schritt 3

- Neue Felder: am Auftrag `customerOrderNumber` (BT-13), `contractNumber` (BT-12), `customerCostCenter` (BT-19),
  `customerContactPerson/Department/Email` (BT-56/58); an der Position `customerOrderNumber`, `customerCostCenter`;
  am Zahlplan `periodOfPerformanceBegin/End` (BT-73/74). Migration `V9.2.13.1__RELEASE-OrderEInvoiceFields.sql`.
- „Aus Konto ergänzen“: `POST /rs/order/customerContactFromAccount` liefert nur die drei Kontaktwerte des Kunden-Kontos
  und füllt nur Leeres; Voraussetzung ist das Recht, Aufträge anzulegen.
- Bewusst nicht ergänzt: Orderbook-Snapshots (`orderbooksnapshots/*`) und die Excel-Exporte (`OrderExport`,
  `ForecastExport`); sie brauchen die Rechnungsdaten nicht. Bei Bedarf nachziehen.
