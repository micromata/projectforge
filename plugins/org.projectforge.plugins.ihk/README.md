# IHK Excel Export

Dieses Plugin exportiert die Wochenberichte in eine Excel-Tabelle zum herunterladen.  

##Einrichtung
Um es zu benutzen muss man es ein json in der Adresse des Benutzers anlegen.  
Das json kommt in die Bemerkung des Benutzers hinein und hat folgendes Format:  
 ``` JSON
 { 
  "ausbildungsbeginn" : "yyyy-mm-dd", 
  "ausbildungsjahr" : "-1", 
  "teamname" : "Dein Teamname" 
}
```  
 
 
 Das Ausbildungsjahr kann man überschreiben sofern man es nicht auf `"-1"` setzt (nötig für jene, die Verkürzen oder das erste Ausbildungsjahr überspringen)