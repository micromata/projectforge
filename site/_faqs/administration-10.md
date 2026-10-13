---
title: Start-up of ProjectForge fails due to Lucene errors?
categories: [administration,faq]
permalink: /misc/faqs.html
---

Probably ProjectForge (and with it the search indexer) wasn't shut down properly. Stop ProjectForge, delete the `hibernateSearch` directory in the ProjectForge home directory and start ProjectForge again. Then rebuild the search index: as an administrator, choose *Re-build whole search index* in the gear menu of a list.
