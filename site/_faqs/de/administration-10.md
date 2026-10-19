---
title: ProjectForge startet wegen Lucene-Fehlern nicht?
categories: [administration,faq]
permalink: /de/misc/faqs.html
---

Vermutlich wurde ProjectForge (und damit der Suchindexer) nicht sauber beendet. ProjectForge stoppen, das Verzeichnis `hibernateSearch` im ProjectForge-Home-Verzeichnis löschen und ProjectForge wieder starten. Danach den Suchindex neu aufbauen: als Administrator im Zahnrad-Menü einer Liste *Suchindex voll indizieren* wählen.
