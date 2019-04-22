/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.fibu;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.kost.Kost1DO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberHelper;

public class KostFormatter {
  public static final int MAX_VALUE = 99999999;

  /**
   * @return Id im dreistelligen Format: "001" - "999" oder "???" wenn id null ist.
   * @see #format3Digits(Integer)
   */
  public static String format(final KundeDO kunde) {
    if (kunde == null) {
      return "???";
    }
    return format3Digits(kunde.getId());
  }

  /**
   * Ruft format(projekt, false) auf.
   *
   * @return "5." + format(kunde) + "." + 3 stellige Projekt-Id;
   * @see #format(ProjektDO, boolean)
   */
  public static String format(final ProjektDO projekt) {
    return format(projekt, false);
  }

  /**
   * @param numberFormat Wenn true, dann werden die Trennpunkte nicht angegeben.
   * @return nummernkreis + format(kunde)/projekt.getBereich() + "." + 3 stellige Projekt-Id;
   * @see StringUtils#leftPad(String, int, char)
   * @see #format(KundeDO)
   */
  public static String format(final ProjektDO projekt, final boolean numberFormat) {
    if (projekt == null) {
      return "?.???.???";
    }
    final String delimiter = (numberFormat == true) ? "" : ".";
    final StringBuffer buf = new StringBuffer();
    buf.append(String.valueOf(projekt.getNummernkreis())).append(delimiter);
    if (projekt.getKunde() != null) {
      buf.append(format(projekt.getKunde()));
    } else {
      buf.append(format3Digits(projekt.getBereich()));
    }
    buf.append(delimiter).append(format2Digits(projekt.getNummer()));
    return buf.toString();
  }

  /**
   * Gibt vollständige Projektnummer inkl. Kundennummer aus ("5.xxx.xxx") und hängt den Kundennamen (max. 30 Zeichen)
   * und die Projektbezeichnung (max. 30 Zeichen) an, z. B. "5.123.566 - ABC : ABC e-datagate"
   *
   * @param projekt
   */
  public static String formatProjekt(final ProjektDO projekt) {
    if (projekt == null) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    buf.append(format(projekt));
    if (projekt.getKunde() != null) {
      buf.append(" - ").append(StringUtils.abbreviate(projekt.getKunde().getName(), 30)).append(": ");
    } else {
      buf.append(" - ");
    }
    buf.append(StringUtils.abbreviate(projekt.getName(), 30));
    return buf.toString();
  }

  /**
   * Gibt vollständige Kundennummer aus ("5.xxx") und hängt den Kundennamen (max. 30 stellig) an, z. B.
   * "5.120 - ABC Verwaltungs GmbH"
   *
   * @param kunde
   */
  public static String formatKunde(final KundeDO kunde) {
    if (kunde == null) {
      return "";
    }
    return format(kunde) + " - " + StringUtils.abbreviate(kunde.getName(), 30);
  }

  /**
   * Displays kunde and kundeText (kunde or kundeText may be null).
   *
   * @param kunde
   * @param kundeText
   * @return formatKunde(kunde), kundeText
   */
  public static String formatKunde(final KundeDO kunde, final String kundeText) {
    final StringBuffer buf = new StringBuffer();
    if (kunde != null) {
      buf.append(formatKunde(kunde));
    }
    if (StringUtils.isNotBlank(kundeText) == true) {
      if (kunde != null) {
        buf.append(", ");
      }
      buf.append(kundeText);
    }
    return buf.toString();
  }

  /**
   * Calls format(kost2, false)
   *
   * @param kost2
   * @see #format(Kost2DO, boolean)
   */
  public static String format(final Kost2DO kost2) {
    return format(kost2, false);
  }

  /**
   * @param kost2
   * @param abbreviate The complete result is abbreviated.
   * @return "5.120.23.02 - travel - project" or "6.100.27.03 - {cost 2 description}
   */
  public static String format(final Kost2DO kost2, final int abbreviate) {
    if (kost2 == null) {
      return "";
    }
    final StringBuilder sb = new StringBuilder();
    sb.append(kost2.getNummernkreis()).append(".").append(format3Digits(kost2.getBereich())).append(".")
            .append(format2Digits(kost2.getTeilbereich())).append(".");
    if (kost2.getKost2Art() != null) {
      sb.append(format2Digits(kost2.getKost2Art().getId()));
    } else {
      sb.append("--");
    }
    sb.append(": ");
    if (kost2.getProjekt() != null) {
      if (kost2.getKost2Art() != null) {
        sb.append(kost2.getKost2Art().getName());
      }
      sb.append(" - ").append(kost2.getProjekt().getName());
    } else {
      sb.append(kost2.getDescription());
    }
    return StringUtils.abbreviate(sb.toString(), abbreviate);
  }

  /**
   * @param kost2
   * @param numberFormat If false, then delimiter '.' will be used: "#.###.##.##", otherwise unformmatted number will be
   *                     returned: ########.
   * @return
   */
  public static String format(final Kost2DO kost2, final boolean numberFormat) {
    if (kost2 == null) {
      return "";
    }
    final String delimiter = (numberFormat == true) ? "" : ".";
    final StringBuffer buf = new StringBuffer();
    buf.append(kost2.getNummernkreis()).append(delimiter).append(format3Digits(kost2.getBereich())).append(delimiter)
            .append(format2Digits(kost2.getTeilbereich())).append(delimiter);
    if (kost2.getKost2Art() != null) {
      buf.append(format2Digits(kost2.getKost2Art().getId()));
    } else {
      buf.append("--");
    }
    return buf.toString();
  }

  /**
   * Format Kost2DO in form (for displaying tool tips):
   * <ul>
   * <li>Project is given: [description]; [projekt.kunde.name] - [projekt.name]; [kost2Art.id] - [kost2Art.name];</li>
   * <li>Project is not given: [description]</li>
   * </ul>
   * DONT'T forget to escape html if displayed directly!
   *
   * @param kost2
   * @return formatted string or "" if kost2 is null.
   */
  public static String formatToolTip(final Kost2DO kost2) {
    if (kost2 == null) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    if (StringUtils.isNotBlank(kost2.getDescription()) == true) {
      buf.append(kost2.getDescription()).append("; ");
    }
    if (kost2.getProjekt() != null) {
      if (kost2.getProjekt().getKunde() != null) {
        buf.append(kost2.getProjekt().getKunde().getKundeIdentifierDisplayName()).append(" - ");
      }
      buf.append(kost2.getProjekt().getProjektIdentifierDisplayName()).append("; ");
      if (kost2.getKost2Art() != null) {
        // Nur, wenn Projekt gegeben ist!
        buf.append(StringHelper.format2DigitNumber(kost2.getKost2Art().getId())).append(" - ")
                .append(kost2.getKost2Art().getName());
      }
    }
    return buf.toString();
  }

  public static String formatLong(final Kost2DO kost2) {
    if (kost2 == null) {
      return "";
    }
    return format(kost2) + " - " + formatToolTip(kost2);
  }

  /**
   * Format for using in e. g. combo boxes for selection:
   * <ul>
   * <li>Project is given: #.###.##.## - [kost2Art.name];</li>
   * <li>Project is not given: #.###.##.## - [description]</li>
   * </ul>
   *
   * @return
   */
  public static String formatForSelection(final Kost2DO kost2) {
    if (kost2 == null) {
      return "";
    }
    final StringBuffer buf = new StringBuffer();
    buf.append(format(kost2));
    if (kost2.getProjekt() != null) {
      buf.append(" - ").append(kost2.getKost2Art().getName());
      if (kost2.getKost2Art().isFakturiert() == false) {
        buf.append(" (nf)");
      }
    } else {
      buf.append(" - ").append(kost2.getDescription());
    }
    return buf.toString();
  }

  /**
   * Calls format(kost1, false)
   *
   * @param kost1
   * @see #format(Kost1DO, boolean)
   */
  public static String format(final Kost1DO kost1) {
    return format(kost1, false);
  }

  public static String format(final Kost1DO kost1, final boolean numberFormat) {
    if (kost1 == null) {
      return "";
    }
    final String delimiter = (numberFormat == true) ? "" : ".";
    final StringBuffer buf = new StringBuffer();
    buf.append(kost1.getNummernkreis()).append(delimiter).append(format3Digits(kost1.getBereich())).append(delimiter)
            .append(format2Digits(kost1.getTeilbereich())).append(delimiter).append(format2Digits(kost1.getEndziffer()));
    return buf.toString();
  }

  /**
   * @param kost1
   * @return Description
   * @see Kost1DO#getDescription()
   */
  public static String formatToolTip(final Kost1DO kost1) {
    if (kost1 == null) {
      return "";
    }
    return kost1.getDescription();
  }

  /**
   * Gibt den Kostenträger als Ganzzahl zurück. Wenn die Wertebereiche der einzelnen Parameter außerhalb des definierten
   * Bereichs liegt, wird eine UnsupportedOperationException geworfen.
   *
   * @param nummernkreis Muss zwischen 1 und 9 inklusive liegen.
   * @param bereich      Muss ziwschen 0 und 999 inklusive liegen.
   * @param teilbereich  Muss zwischen 0 und 99 inklusive liegen.
   * @param endziffer    Muss zwischen 0 und 99 inklusive liegen.
   * @return
   */
  public static int getKostAsInt(final int nummernkreis, final int bereich, final int teilbereich, final int endziffer) {
    if (nummernkreis < 1 || nummernkreis > 9) {
      throw new UnsupportedOperationException("Nummernkreis muss zwischen 1 und 9 liegen: '" + nummernkreis + "'.");
    }
    if (bereich < 0 || bereich > 999) {
      throw new UnsupportedOperationException("Bereich muss zwischen 0 und 999 liegen: '" + bereich + "'.");
    }
    if (teilbereich < 0 || teilbereich > 99) {
      throw new UnsupportedOperationException("Teilbereich muss zwischen 0 und 99 liegen: '" + teilbereich + "'.");
    }
    if (endziffer < 0 || endziffer > 99) {
      throw new UnsupportedOperationException("Endziffer muss zwischen 0 und 99 liegen: '" + teilbereich + "'.");
    }
    final int result = nummernkreis * 10000000 + bereich * 10000 + teilbereich * 100 + endziffer;
    return result;
  }

  /**
   * If not given, then ?? will be returned.
   *
   * @param number
   * @return
   * @see StringUtils#leftPad(String, int, char)
   */
  public static String format2Digits(final Integer number) {
    if (number == null) {
      return "??";
    }
    return StringUtils.leftPad(number.toString(), 2, '0');
  }

  /**
   * If not given, then ??? will be returned.
   *
   * @param number
   * @return
   * @see StringUtils#leftPad(String, int, char)
   */
  public static String format3Digits(final Integer number) {
    if (number == null) {
      return "???";
    }
    return StringUtils.leftPad(number.toString(), 3, '0');
  }

  /**
   * @param year
   * @param month 0 (January) - 11 (December)
   * @return
   */
  public static String formatBuchungsmonat(final int year, final int month) {
    return DateHelper.formatMonth(year, month);
  }

  /**
   * Return the values for nummernkreis, bereich, teilbereich and endziffer / kost2Art.
   *
   * @param kost in format #.###.##.## (e. g.: 1.623.23.12).
   * @return int[4] containing nummernkreis, bereich, teilbereich and endziffer / kost2Art if exists.
   * @see StringHelper#splitToInts(String, String)
   */
  public static int[] splitKost(final String kost) {
    final int[] result = StringHelper.splitToInts(kost, ".");
    if (result.length != 4) {
      throw new UnsupportedOperationException("Unsupported format of Kost: " + kost);
    }
    return result;
  }

  /**
   * Uses NumberHelper.splitToInts(value, 1, 3, 2, 2)
   *
   * @param value
   * @see NumberHelper#splitToInts(Number, int...)
   */
  public static int[] splitKost(final Number value) {
    final int[] result = NumberHelper.splitToInts(value, 1, 3, 2, 2);
    if (value.intValue() > MAX_VALUE) {
      throw new UnsupportedOperationException("Unsupported format of Kost (max value = " + MAX_VALUE + ": " + value);
    }
    if (result.length != 4) {
      throw new UnsupportedOperationException("Unsupported format of Kost: " + value);
    }
    return result;
  }

  public static String formatKost(final Number value) {
    final int[] a = splitKost(value);
    return a[0] + "." + format3Digits(a[1]) + "." + format2Digits(a[2]) + "." + format2Digits(a[3]);
  }

  public static String formatNummer(final KontoDO konto) {
    return StringUtils.leftPad(String.valueOf(konto.getNummer()), 5);
  }

  public static String formatZeitraum(final int fromYear, final int fromMonth, final int toYear, final int toMonth) {
    final StringBuffer buf = new StringBuffer();
    if (fromYear > 0) {
      buf.append(formatBuchungsmonat(fromYear, fromMonth));
      if (toYear > 0) {
        buf.append(" - ");
        buf.append(formatBuchungsmonat(toYear, toMonth));
      }
    }
    return buf.toString();
  }
}
