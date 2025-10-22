# Availability I18n Keys - OPTIMIERT (Minimale neue Keys)

Diese Datei enthält **NUR** die wirklich benötigten neuen I18n-Keys für das Availability-System.
**Viele Keys werden von Vacation wiederverwendet!**

## ✅ Wiederverwendete Keys (KEINE Änderungen nötig)

Diese Keys existieren bereits und werden wiederverwendet:

```properties
# Von Vacation wiederverwendet:
vacation.employee=Mitarbeiter
vacation.startdate=Startdatum
vacation.enddate=Enddatum
vacation.replacement=Vertretung
vacation.replacement.others=Weitere Vertretungen
vacation.conflicts=Konflikte

# Allgemeine Keys:
comment=Bemerkung
timePeriod=Zeitraum
calendar.year=Jahr
fibu.employee=Mitarbeiter:in
```

## ❗ Neue Keys die hinzugefügt werden müssen

### Deutsche Keys (I18nResources_de.properties)

```properties
# Availability - Haupttitel und spezifische Felder (5 Keys)
availability.title=Verfügbarkeiten
availability.type=Typ
availability.percentage=Prozent
availability.conflict.info=Es besteht ein Konflikt mit den Abwesenheiten der Vertreter:innen. An mindestens einem Tag steht kein:e Vertreter:in zur Verfügung. Bitte andere oder weitere Vertretungen auswählen.
availability.availabilitiesOfReplacements=Abwesenheiten der Vertretungen

# Availability Types (4 Typen)
availability.type.absent=Abwesenheit (nicht verfügbar)
availability.type.remote=Remote (erreichbar)
availability.type.partialAbsence=Teilzeitabwesenheit
availability.type.parentalLeave=Elternzeit (nicht verfügbar)

# Reachability Status (3 Stati)
availability.reachabilityStatus.unavailable=Nicht verfügbar
availability.reachabilityStatus.available=Verfügbar
availability.reachabilityStatus.limited=Eingeschränkt verfügbar

# Fehler (6 Fehler)
availability.error.invalidDateRange=Ungültiger Datumsbereich
availability.error.missingEmployee=Mitarbeiter fehlt
availability.error.missingType=Typ fehlt
availability.error.invalidType=Ungültiger Typ
availability.error.missingReplacement=Vertretung fehlt
availability.error.invalidPercentage=Ungültiger Prozentsatz (0-100)

# Access Exception (1 Key)
access.exception.hrOnlyAvailabilityType=Nur HR darf Einträge dieses Typs erstellen oder bearbeiten
```

### Englische Keys (I18nResources.properties)

```properties
# Availability - Haupttitel und spezifische Felder (5 Keys)
availability.title=Availabilities
availability.type=Type
availability.percentage=Percentage
availability.conflict.info=There is a conflict with the absences of the replacements. At least one replacement is not available on at least one day. Please select other or additional replacements.
availability.availabilitiesOfReplacements=Availabilities of Replacements

# Availability Types (4 Typen)
availability.type.absent=Absent (unavailable)
availability.type.remote=Remote (available)
availability.type.partialAbsence=Partial Absence
availability.type.parentalLeave=Parental Leave (unavailable)

# Reachability Status (3 Stati)
availability.reachabilityStatus.unavailable=Unavailable
availability.reachabilityStatus.available=Available
availability.reachabilityStatus.limited=Limited Availability

# Fehler (6 Fehler)
availability.error.invalidDateRange=Invalid date range
availability.error.missingEmployee=Employee missing
availability.error.missingType=Type missing
availability.error.invalidType=Invalid type
availability.error.missingReplacement=Replacement missing
availability.error.invalidPercentage=Invalid percentage (0-100)

# Access Exception (1 Key)
access.exception.hrOnlyAvailabilityType=Only HR can create or edit entries of this type
```

## Zusammenfassung

**✅ Wiederverwendet:** 10 Keys von Vacation/Allgemein
**❗ Neu hinzugefügt:** Nur 19 Keys (5 + 4 + 3 + 6 + 1)

**Ersparnis:** ~35% weniger neue Keys durch intelligente Wiederverwendung!

## application.properties - Default Configuration

```properties
# Availability Types Configuration
projectforge.availability.types[0].key=ABSENT
projectforge.availability.types[0].i18nKey=availability.type.absent
projectforge.availability.types[0].color=#DC3545
projectforge.availability.types[0].reachabilityStatus=UNAVAILABLE
projectforge.availability.types[0].percentage=100
projectforge.availability.types[0].hrOnly=false

projectforge.availability.types[1].key=REMOTE
projectforge.availability.types[1].i18nKey=availability.type.remote
projectforge.availability.types[1].color=#17A2B8
projectforge.availability.types[1].reachabilityStatus=AVAILABLE
projectforge.availability.types[1].percentage=
projectforge.availability.types[1].hrOnly=false

projectforge.availability.types[2].key=PARTIAL_ABSENCE
projectforge.availability.types[2].i18nKey=availability.type.partialAbsence
projectforge.availability.types[2].color=#FFC107
projectforge.availability.types[2].reachabilityStatus=LIMITED
projectforge.availability.types[2].percentage=50
projectforge.availability.types[2].hrOnly=false

projectforge.availability.types[3].key=PARENTAL_LEAVE
projectforge.availability.types[3].i18nKey=availability.type.parentalLeave
projectforge.availability.types[3].color=#6C757D
projectforge.availability.types[3].reachabilityStatus=UNAVAILABLE
projectforge.availability.types[3].percentage=100
projectforge.availability.types[3].hrOnly=true
```

## Code-Änderungen durchgeführt

Die folgenden Anpassungen wurden bereits im Code vorgenommen:

### AvailabilityDO.kt
- `@PropertyInfo(i18nKey = "vacation.employee")`
- `@PropertyInfo(i18nKey = "vacation.startdate")`
- `@PropertyInfo(i18nKey = "vacation.enddate")`
- `@PropertyInfo(i18nKey = "vacation.replacement")`
- `@PropertyInfo(i18nKey = "vacation.replacement.others")`

### AvailabilityPagesRest.kt
- `translate("vacation.employee")`
- `translate("vacation.replacement")`
- `translate("vacation.conflicts")`
- `translate("timePeriod")`
- `translate("calendar.year")`
- `translate("comment")`

### AvailabilityProvider.kt
- `translate("vacation.replacement")`
- `translate("timePeriod")`
- `translate("comment")`
