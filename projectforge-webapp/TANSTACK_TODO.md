# TanStack Table — Offene Punkte

Vergleich der alten AG-Grid-Implementierung mit der aktuellen TanStack-Implementierung.
Stand: 2026-07-28

## Priorität 1: Funktional kritisch

### Multi-Row-Selection (Checkboxen)
- [x] Checkbox-Spalte rendern (TanStack `enableRowSelection`)
- [x] `selectedEntities` vom Backend wiederherstellen (pre-checked rows)
- [x] Cancel/Next-Buttons mit tatsächlicher Selection verknüpfen
- [x] Klick, Shift+Klick (Range), Ctrl/Cmd+Klick (additiv), Pfeiltasten+Shift, Leertaste
- [ ] Testen mit Massenbearbeitung (Timesheets), Multi-Select-Seiten
- Dateien: `DynamicListPageTanStackGrid.tsx`, `DynamicTanStackGrid.tsx`

### filterModel-Wiederherstellung
- [x] Geprüft: Backend schickt immer leere filterModel → kein Handlungsbedarf
- Datei: `DynamicTanStackGrid.tsx`

## Priorität 2: Sichtbare UX-Lücken

### Cell-Tooltips (tooltipField)
- [x] `tooltipField` aus Column-Meta lesen
- [x] Als `title`-Attribut auf `<td>` rendern
- Datei: `DynamicTanStackGrid.tsx` (im Cell-Rendering)

### Numerische Spaltenausrichtung
- [x] `type: "numericColumn"` oder `"rightAligned"` → `text-align: right` auf `<th>` und `<td>`
- [x] `type` im `meta` der Column-Defs gespeichert
- Datei: `DynamicTanStackGrid.tsx`, `tableUtils.ts`

### valueFormatter
- [x] Geprüft: Wird nur für ADDRESS_BOOK genutzt (Array → displayName join)
- [x] CellRendererDispatch handelt Arrays mit displayName bereits nativ (Zeile 27-29)
- [x] Kein zusätzlicher Code nötig
- Dateien: `CellRendererDispatch.tsx`

## Priorität 3: Kleinere Lücken

### rowClickPostUrl
- [x] POST an `${rowClickPostUrl}/${row.id}` bei Zeilen-Klick
- [x] Response wird über `callAction({ responseAction: json })` verarbeitet
- Datei: `DynamicTanStackGrid.tsx`
- Testen: EInvoiceCheckerPageRest (Attachment-Download), MerlinPagesRest (Variable bearbeiten)

### lockPosition im Header-DnD
- [x] Angepinnte Spalten im Header-Drag-and-Drop nicht verschiebbar (draggable=false, Drop ignored)
- Datei: `DynamicTanStackGrid.tsx`

### Typed Filter (Optional)
- [ ] Aktuell nur Set-Filter (Checkbox-Liste)
- [ ] Optional: Number-Range-Filter, Date-Picker-Filter, Text-Contains-Filter
- [ ] AG-Grid hatte: `agNumberColumnFilter`, `agDateColumnFilter`, `agTextColumnFilter`
- [ ] Für MVP reicht der Set-Filter — typed Filter nur bei Bedarf nachziehen

### autoHeight
- [x] Funktioniert implizit: HTML-Tabellen passen Zeilenhöhe automatisch an
- [x] `wrapText` → `white-space: pre-line` ist bereits implementiert
- Kein zusätzlicher Code nötig

### Grid-Container-Höhe (height)
- [x] Geprüft: `height`-Prop wird im Backend nirgends gesetzt (nur auskommentiert)
- [x] Kein Handlungsbedarf

## Nicht benötigt

Folgende AG-Grid Enterprise Features waren nie konfiguriert und werden nicht nachgebaut:
- Pivot / Row Grouping
- Cell Editing (Inline-Bearbeitung)
- Clipboard / Context Menu
- Server-seitiges Filtern/Sortieren
- Column Tool Panel Sidebar (ersetzt durch einfacheres Panel)
- Cell Range Selection
