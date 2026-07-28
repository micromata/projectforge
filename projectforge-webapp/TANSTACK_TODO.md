# TanStack Table — Offene Punkte

Vergleich der alten AG-Grid-Implementierung mit der aktuellen TanStack-Implementierung.
Stand: 2026-07-28

## Priorität 1: Funktional kritisch

### Multi-Row-Selection (Checkboxen)
- [x] Checkbox-Spalte rendern (TanStack `enableRowSelection`)
- [x] `selectedEntities` vom Backend wiederherstellen (pre-checked rows)
- [x] Cancel/Next-Buttons mit tatsächlicher Selection verknüpfen
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
- [ ] Backend schickt JS-Expressions (z.B. für ADDRESS_BOOK)
- [ ] Dot-Path-Auswertung analog zu `valueGetter`, aber für die Anzeige
- [ ] Fallback: Wenn `cellRenderer: "formatter"` gesetzt ist, wird das bereits über den Formatter abgedeckt — prüfen welche Spalten `valueFormatter` ohne `cellRenderer` nutzen
- Dateien: `CellRendererDispatch.tsx`, `tableUtils.ts`

## Priorität 3: Kleinere Lücken

### rowClickPostUrl
- [ ] POST an URL bei Zeilen-Klick (statt Navigation)
- [ ] Analog zu `rowClickRedirectUrl`, aber mit fetch POST
- Datei: `DynamicTanStackGrid.tsx`

### lockPosition im Header-DnD
- [x] Angepinnte Spalten im Header-Drag-and-Drop nicht verschiebbar (draggable=false, Drop ignored)
- Datei: `DynamicTanStackGrid.tsx`

### Typed Filter (Optional)
- [ ] Aktuell nur Set-Filter (Checkbox-Liste)
- [ ] Optional: Number-Range-Filter, Date-Picker-Filter, Text-Contains-Filter
- [ ] AG-Grid hatte: `agNumberColumnFilter`, `agDateColumnFilter`, `agTextColumnFilter`
- [ ] Für MVP reicht der Set-Filter — typed Filter nur bei Bedarf nachziehen

### autoHeight
- [ ] Zeilen automatisch an Inhalt anpassen wenn `autoHeight: true`
- [ ] Aktuell: `white-space: nowrap` + `overflow: hidden` auf allen Zellen
- [ ] Bei `autoHeight`/`wrapText`: `white-space: pre-line` wird schon gesetzt, aber Zeilenhöhe ist fix

### Grid-Container-Höhe (height)
- [ ] `height`-Prop vom Backend wird ignoriert
- [ ] Ggf. `max-height` + `overflow-y: auto` auf `.table-responsive`

## Nicht benötigt

Folgende AG-Grid Enterprise Features waren nie konfiguriert und werden nicht nachgebaut:
- Pivot / Row Grouping
- Cell Editing (Inline-Bearbeitung)
- Clipboard / Context Menu
- Server-seitiges Filtern/Sortieren
- Column Tool Panel Sidebar (ersetzt durch einfacheres Panel)
- Cell Range Selection
