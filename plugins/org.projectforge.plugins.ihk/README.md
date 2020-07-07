# IHK Excel Export

Dieses Plugin exportiert die Wochenberichte in eine Excel-Tabelle zum Herunterladen.  

## Einrichtung

Um es zu benutzen muss man es ein JSON-Objekt in den Kontakten anlegen.  
Das JSON kommt in das Bemerkungsfeld des Benutzers hinein und hat folgendes Format:  
 
``` JSON
 { 
  "ausbildungsbeginn" : "yyyy-mm-dd", 
  "ausbildungsjahr" : "-1", 
  "teamname" : "Dein Teamname" 
}
```  

Dabei ist zu beachten, dass der Vor- und Nachname des Benutzers mit dem des Kontakts übereinstimmen muss

Das Ausbildungsjahr kann man überschreiben sofern man es nicht auf `"-1"` setzt (nötig für jene, die verkürzen oder das erste Ausbildungsjahr überspringen)
