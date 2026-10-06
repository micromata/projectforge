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
`bin/pfDev.sh gen` validates the entries and shows them on `/next/changelog` as "Not yet released".
`bin/pfDev.sh release X.Y.Z` moves them into the release entry on top of `changelog.json` and
`changelog.de.json` (it needs `id`, `version`, `date`, `title` and `tag`, in `changelog.de.json` the `title`; sections of
its own are optional) and deletes the files. `bin/pfDev.sh changelog-fold [id]` does the same by hand, e.g. for a
snapshot entry.
