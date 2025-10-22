# Availability System - Customization Guide

Dieses Dokument beschreibt, wie Administratoren und Kunden das Availability-System an ihre Bedürfnisse anpassen können.

## Übersicht

Das Availability-System nutzt ProjectForges flexibles I18n-System mit **CustomerI18nResources**-Support. Das bedeutet:
- ✅ Übersetzungen können **ohne Code-Änderungen** angepasst werden
- ✅ Neue Availability-Typen können einfach hinzugefügt werden
- ✅ Unterstützt beliebig viele Sprachen

## 1. Übersetzungen anpassen

### Verwendung von CustomerI18nResources

ProjectForge lädt I18n-Keys in folgender Priorität:
1. **CustomerI18nResources** (höchste Priorität) - aus `~/ProjectForge/resources/`
2. I18nResources (Standard) - aus Classpath

### Schritt 1: CustomerI18nResources erstellen

Erstellen Sie folgende Dateien im `resourceDir` (Standard: `~/ProjectForge/resources/`):

```bash
~/ProjectForge/resources/CustomerI18nResources.properties        # Englisch (Default)
~/ProjectForge/resources/CustomerI18nResources_de.properties     # Deutsch
```

### Schritt 2: Übersetzungen überschreiben

**Beispiel: `CustomerI18nResources_de.properties`**
```properties
# Standard-Typen anpassen
availability.type.absent=Nicht im Büro
availability.type.remote=Mobiles Arbeiten
availability.type.partialAbsence=Teilzeitarbeit
availability.type.parentalLeave=Elternzeit

# Weitere Begriffe anpassen
availability.title=Anwesenheitsstatus
availability.percentage=Verfügbarkeit in %
```

**Beispiel: `CustomerI18nResources.properties`**
```properties
# English customization
availability.type.absent=Not in Office
availability.type.remote=Working from Home
availability.type.partialAbsence=Part-Time Work
availability.type.parentalLeave=Parental Leave

availability.title=Availability Status
```

### Schritt 3: ProjectForge neu starten

Die CustomerI18nResources werden beim Start automatisch erkannt und geladen.

## 2. Neue Availability-Typen hinzufügen

### Schritt 1: Typ in application.properties konfigurieren

```properties
# Beispiel: Firmenevent hinzufügen
projectforge.availability.types[4].key=COMPANY_EVENT
projectforge.availability.types[4].i18nKey=availability.type.companyEvent
projectforge.availability.types[4].color=#FFD700
projectforge.availability.types[4].reachabilityStatus=UNAVAILABLE
projectforge.availability.types[4].percentage=100
projectforge.availability.types[4].hrOnly=false

# Beispiel: Krankmeldung hinzufügen
projectforge.availability.types[5].key=SICK_LEAVE
projectforge.availability.types[5].i18nKey=availability.type.sickLeave
projectforge.availability.types[5].color=#FF5733
projectforge.availability.types[5].reachabilityStatus=UNAVAILABLE
projectforge.availability.types[5].percentage=100
projectforge.availability.types[5].hrOnly=false

# Beispiel: Kundentermin (erreichbar)
projectforge.availability.types[6].key=CLIENT_MEETING
projectforge.availability.types[6].i18nKey=availability.type.clientMeeting
projectforge.availability.types[6].color=#28A745
projectforge.availability.types[6].reachabilityStatus=LIMITED
projectforge.availability.types[6].hrOnly=false
```

### Schritt 2: Übersetzungen in CustomerI18nResources hinzufügen

**`CustomerI18nResources_de.properties`:**
```properties
availability.type.companyEvent=Firmenevent
availability.type.sickLeave=Krankmeldung
availability.type.clientMeeting=Kundentermin
```

**`CustomerI18nResources.properties`:**
```properties
availability.type.companyEvent=Company Event
availability.type.sickLeave=Sick Leave
availability.type.clientMeeting=Client Meeting
```

### Schritt 3: ProjectForge neu starten

Die neuen Typen erscheinen automatisch in der Auswahlliste.

## 3. Konfigurationsoptionen

### AvailabilityTypeConfig Properties

| Property | Typ | Beschreibung | Beispiel |
|----------|-----|--------------|----------|
| `key` | String | Eindeutiger Schlüssel (UPPERCASE_WITH_UNDERSCORE) | `ABSENT` |
| `i18nKey` | String | I18n-Key für Label | `availability.type.absent` |
| `color` | String | Hex-Farbe für Kalenderanzeige | `#DC3545` |
| `reachabilityStatus` | String | UNAVAILABLE, AVAILABLE, LIMITED | `UNAVAILABLE` |
| `percentage` | Integer | Prozentuale Verfügbarkeit (optional) | `50` |
| `hrOnly` | Boolean | Nur HR darf diesen Typ erstellen/bearbeiten | `false` |

### ReachabilityStatus

- **UNAVAILABLE**: Nicht erreichbar (z.B. Urlaub, Abwesenheit)
- **AVAILABLE**: Voll erreichbar (z.B. Remote-Arbeit)
- **LIMITED**: Eingeschränkt erreichbar (z.B. Kundentermin, Teilzeit)

### Farben

Empfohlene Farben für gute Sichtbarkeit:
- Rot (`#DC3545`): Nicht verfügbar
- Blau (`#17A2B8`): Remote
- Gelb (`#FFC107`): Teilzeit/Eingeschränkt
- Grau (`#6C757D`): Langfristige Abwesenheit
- Grün (`#28A745`): Verfügbar mit Einschränkung

## 4. Standard-Konfiguration

Die Standard-Availability-Typen sind:

```properties
# 1. Absent (Abwesenheit)
projectforge.availability.types[0].key=ABSENT
projectforge.availability.types[0].i18nKey=availability.type.absent
projectforge.availability.types[0].color=#DC3545
projectforge.availability.types[0].reachabilityStatus=UNAVAILABLE
projectforge.availability.types[0].percentage=100
projectforge.availability.types[0].hrOnly=false

# 2. Remote (Remote-Arbeit)
projectforge.availability.types[1].key=REMOTE
projectforge.availability.types[1].i18nKey=availability.type.remote
projectforge.availability.types[1].color=#17A2B8
projectforge.availability.types[1].reachabilityStatus=AVAILABLE
projectforge.availability.types[1].hrOnly=false

# 3. Partial Absence (Teilzeitabwesenheit)
projectforge.availability.types[2].key=PARTIAL_ABSENCE
projectforge.availability.types[2].i18nKey=availability.type.partialAbsence
projectforge.availability.types[2].color=#FFC107
projectforge.availability.types[2].reachabilityStatus=LIMITED
projectforge.availability.types[2].percentage=50
projectforge.availability.types[2].hrOnly=false

# 4. Parental Leave (Elternzeit) - HR-only
projectforge.availability.types[3].key=PARENTAL_LEAVE
projectforge.availability.types[3].i18nKey=availability.type.parentalLeave
projectforge.availability.types[3].color=#6C757D
projectforge.availability.types[3].reachabilityStatus=UNAVAILABLE
projectforge.availability.types[3].percentage=100
projectforge.availability.types[3].hrOnly=true
```

## 5. Beispiel-Szenarien

### Szenario 1: Firmenspezifische Begriffe

**Anforderung:** Firma möchte "Mobiles Arbeiten" statt "Remote" verwenden.

**Lösung:**
```properties
# CustomerI18nResources_de.properties
availability.type.remote=Mobiles Arbeiten
```

### Szenario 2: Zusätzlicher Typ "Weiterbildung"

**application.properties:**
```properties
projectforge.availability.types[4].key=TRAINING
projectforge.availability.types[4].i18nKey=availability.type.training
projectforge.availability.types[4].color=#9C27B0
projectforge.availability.types[4].reachabilityStatus=LIMITED
projectforge.availability.types[4].hrOnly=false
```

**CustomerI18nResources_de.properties:**
```properties
availability.type.training=Weiterbildung
```

**CustomerI18nResources.properties:**
```properties
availability.type.training=Training
```

### Szenario 3: Mehrsprachigkeit (Deutsch, Englisch, Französisch)

**CustomerI18nResources_de.properties:**
```properties
availability.type.absent=Abwesenheit
availability.title=Verfügbarkeiten
```

**CustomerI18nResources.properties (English):**
```properties
availability.type.absent=Absent
availability.title=Availabilities
```

**CustomerI18nResources_fr.properties:**
```properties
availability.type.absent=Absence
availability.title=Disponibilités
```

## 6. Troubleshooting

### Problem: Übersetzungen werden nicht angezeigt

**Lösung:**
1. Prüfen Sie den Pfad: `~/ProjectForge/resources/CustomerI18nResources_de.properties`
2. Prüfen Sie die Dateinamen (Case-sensitive!)
3. Starten Sie ProjectForge neu
4. Prüfen Sie Logs: `INFO  o.p.f.i.I18nServiceImpl - Customer i18n bundle detected`

### Problem: Neuer Typ erscheint nicht

**Lösung:**
1. Prüfen Sie die Array-Indizierung: `[4]`, `[5]`, etc.
2. Stellen Sie sicher, dass `i18nKey` in CustomerI18nResources definiert ist
3. Starten Sie ProjectForge neu

### Problem: Falsche Farben im Kalender

**Lösung:**
1. Verwenden Sie gültige Hex-Farben: `#RRGGBB`
2. Verwenden Sie keine Transparenz (RGBA)

## 7. Best Practices

✅ **DO:**
- Verwenden Sie sprechende Keys: `COMPANY_EVENT` statt `TYPE4`
- Definieren Sie alle Sprachen in CustomerI18nResources
- Verwenden Sie konsistente Farben für ähnliche Typen
- Dokumentieren Sie Ihre Custom-Typen

❌ **DON'T:**
- Ändern Sie nicht die Standard-Keys (ABSENT, REMOTE, etc.)
- Verwenden Sie keine Sonderzeichen in Keys
- Überschreiben Sie nicht core I18n-Keys ohne Grund

## 8. Support

Bei Fragen oder Problemen:
- 📖 Dokumentation: https://docs.projectforge.org
- 🐛 Issues: https://github.com/micromata/projectforge/issues
- 💬 Community: ProjectForge Forum
