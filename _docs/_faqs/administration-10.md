---
title: Start-up of ProjectForge fails due to Lucene errors.partly (e. g. half)?
categories: [administration]
---

Probably ProjectForge and therefore the indexer wasn't terminated correctly. Please feel free to remove the directories and/or files of Lucene and rebuild index via the administrator's web page. The files are located in the ProjectForge's application dir in the `hibernate-search` dir.
