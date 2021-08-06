---
title: Migration
subtitle: Most migrations (data base as well as data base content) will be done automatically during the start-up phase of an newer version.
author: kai
tags: [migration,config]
---

But there are some really small changes e. g. in config files etc. which have to be done manually, as described below.

### Version 6.x to version 7

1. Configurationparameters changed
    1. `config.xml:holiday.month` is now 1-based (1 - January, ..., 12 - December), was 0-based.
    2. `projectforge.properties:projectforge.defaultFirstDayOfWeek=MONDAY` (full name of weekday: SUNDAY, ...)
2. Configuration parameters moved from `config.xml` to `projectforge.properties`
    1. `config.xml:currencySymbol` -> `projectforge.properties:projectforge.currencySymbol=€`
    2. `config.xml:defaultLocale` -> `projectforge.properties:projectforge.defaultLocale=en`
    3. `config.xml:defaultTimeNotation` -> `projectforge.properties:projectforge.defaultTimeNotation=H24`
    4. `config.xml:firstDayOfWeek` -> `projectforge.properties:projectforge.defaultFirstDayOfWeek=MONDAY`
    5. `config.xml:excelPaperSize` -> `projectforge.properties:projectforge.excelPaperSize=DINA4`
3. PhoneLookupServlet moved from `phoneLookup` to `rsPublic/phoneLookup`
