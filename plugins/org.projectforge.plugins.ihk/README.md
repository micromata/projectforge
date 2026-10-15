# IHK Excel Export

Dieses Plugin exportiert die Wochenberichte in eine Excel-Tabelle zum Herunterladen.  

## Einrichtung

Die Angaben für den Bericht (Ausbildungsbeginn, Ausbildungsjahr, Team) trägt jede:r Auszubildende selbst auf der
IHK-Seite (`/next/ihk`) ein; sie werden als Benutzereinstellung gespeichert und lassen sich dort jederzeit ändern.

Das Ausbildungsjahr wird aus dem Ausbildungsbeginn berechnet, solange kein Jahr gewählt ist (nötig nur für jene, die
verkürzen oder das erste Ausbildungsjahr überspringen).

Früher stand die Einrichtung als JSON-Objekt im Bemerkungsfeld der eigenen Adresse (Vor- und Nachname mussten mit dem
Benutzer übereinstimmen). Wer noch keine Benutzereinstellung hat, dessen JSON wird beim ersten Aufruf der Seite
einmalig übernommen; danach wird die Adresse nicht mehr gelesen.
