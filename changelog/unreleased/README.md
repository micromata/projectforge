# Unreleased changelog entries

One file per change, so that branches working on the same release don't conflict in
`changelog.json`/`changelog.de.json`. The name is `yyyyMMdd-<slug>.json` (the date orders the entries,
the slug is lower case words joined by `-`), the text follows the rules of `changelog.json` (English,
published — no names of persons, customers or internal teams) and comes with its German translation.

An item:

```json
{
  "type": "added",
  "en": "Project list: columns for the last time sheet and the last order date.",
  "de": "Projektliste: Spalten für die letzte Zeitbuchung und das letzte Auftragsdatum."
}
```

A group (one level of sub items):

```json
{
  "type": "changed",
  "en": "Order book",
  "de": "Auftragsbuch",
  "items": [{ "en": "Faster list.", "de": "Schnellere Liste." }]
}
```

`type` is one of the section types of `changelog.json` (`added`, `improved`, `changed`, `fixed`, …).
## The levels of a release: `release.json`

On the website and on `/next/changelog` a release shows three levels: the `summary` (keywords and
important bug fixes, always shown), the `overview` (one line per topic, opens with a click) and all
changes (its sections, from the files above). Summary and overview are written at release time, for the
whole release, in `release.json` here (optionally also its `title` and `intro`):

```json
{
  "summary": {
    "en": ["**Order book** migrated", "Faster invoice list"],
    "de": ["**Auftragsbuch** migriert", "Schnellere Rechnungsliste"]
  },
  "overview": {
    "en": ["Order book: new list and edit page with mass update.", "Invoice list: loads in a second."],
    "de": ["Auftragsbuch: neue Liste und Bearbeitungsseite mit Massenänderung.", "Rechnungsliste: lädt in einer Sekunde."]
  }
}
```

A summary entry is inline text (2–6 short entries), an overview entry follows the rules of an item;
English and German have the same number of entries. `bin/pfDev.sh release` needs both for the release.

## Validation and release

`bin/pfDev.sh gen` validates the entries and shows them on `/next/changelog` as "Not yet released".
`bin/pfDev.sh release X.Y.Z` moves them into the release entry on top of `changelog.json` and
`changelog.de.json` (it needs `id`, `version`, `date`, `title` and `tag`, in `changelog.de.json` the `title`; sections of
its own are optional) and deletes the files, `release.json` included. `bin/pfDev.sh changelog-fold [id]` does the same by hand, e.g. for a
snapshot entry.
