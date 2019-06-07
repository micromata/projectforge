/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.datev;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.framework.time.DayHolder;
import org.projectforge.framework.utils.ActionLog;
import org.projectforge.framework.utils.NumberHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuchungssatzImportRow
{
  private static final Logger log = LoggerFactory.getLogger(BuchungssatzImportRow.class);

  Integer satzNr;

  BigDecimal betrag;

  String sh;

  Integer konto;

  Double kost2;

  String menge;

  String sh2; // Nicht eindeutig, daher umbenannt.

  String beleg;

  Date datum;

  Integer gegenkonto;

  String text;

  Double kost1;

  BigDecimal beleg2;

  String kr_bsnr;

  String zi;

  String comment;

  private ActionLog actionLog;

  public String getBeleg()
  {
    return beleg;
  }

  public void setBeleg(String beleg)
  {
    this.beleg = NumberHelper.toPlainString(beleg); // Convert, because Excelimporter return numeric cells in scientific representation (e.
    // g. 1.093E7).
  }

  public BigDecimal getBeleg2()
  {
    return beleg2;
  }

  public void setBeleg2(BigDecimal beleg2)
  {
    this.beleg2 = beleg2;
  }

  public BigDecimal getBetrag()
  {
    return betrag;
  }

  public void setBetrag(BigDecimal betrag)
  {
    this.betrag = betrag != null ? betrag.setScale(2, RoundingMode.HALF_UP) : null;
  }

  public Date getDatum()
  {
    return datum;
  }

  public void setDatum(Date datum)
  {
    this.datum = datum;
  }

  public Integer getGegenkonto()
  {
    return gegenkonto;
  }

  public void setGegenkonto(Integer gegenkonto)
  {
    this.gegenkonto = gegenkonto;
  }

  public Integer getKonto()
  {
    return konto;
  }

  public void setKonto(Integer konto)
  {
    this.konto = konto;
  }

  public String getKr_bsnr()
  {
    return kr_bsnr;
  }

  public Double getKost1()
  {
    return kost1;
  }

  public void setKost1(Double kost1)
  {
    this.kost1 = kost1;
  }

  public Double getKost2()
  {
    return kost2;
  }

  public void setKost2(Double kost2)
  {
    this.kost2 = kost2;
  }

  public void setKr_bsnr(String kr_bsnr)
  {
    this.kr_bsnr = NumberHelper.toPlainString(kr_bsnr); // See setBeleg for explanation.
  }

  public String getMenge()
  {
    return menge;
  }

  public void setMenge(String menge)
  {
    this.menge = NumberHelper.toPlainString(menge); // See setBeleg for explanation.
  }

  public Integer getSatzNr()
  {
    return satzNr;
  }

  public void setSatzNr(Integer satzNr)
  {
    this.satzNr = satzNr;
  }

  public String getSh()
  {
    return sh;
  }

  public void setSh(String sh)
  {
    this.sh = NumberHelper.toPlainString(sh); // See setBeleg for explanation.
  }

  public String getSh2()
  {
    return sh2;
  }

  public void setSh2(String sh2)
  {
    this.sh2 = NumberHelper.toPlainString(sh2); // See setBeleg for explanation.
  }

  public String getText()
  {
    return text;
  }

  public void setText(String text)
  {
    this.text = text == null ? "" : NumberHelper.toPlainString(text); // See setBeleg for explanation.
  }

  public String getZi()
  {
    return zi;
  }

  public void setZi(String zi)
  {
    this.zi = NumberHelper.toPlainString(zi); // See setBeleg for explanation.
  }

  public String getComment()
  {
    return comment;
  }

  public void setComment(String comment)
  {
    this.comment = NumberHelper.toPlainString(comment); // See setBeleg for explanation.
  }

  public boolean isEmpty()
  {
    return (satzNr == null && betrag == null && konto == null && datum == null && gegenkonto == null && kost1 == null && kost2 == null);
  }

  /**
   * Ein paar Datensätze müssen vor der Übernahme geprüft und ggf. korrigiert werden.
   */
  public void check()
  {
    if (satzNr == null) {
      log.warn("Satznr ist null!");
    } else if (kost1 == null && satzNr.compareTo(1) == 0) {
      final DayHolder day = new DayHolder(datum);
      log.info("OK: Lt. Steffi handelt es sich um einen zu ignorierenden Lohn-Korrektur-Datensatz für 01/2007-05/2007."
          + " Kostenstelle wird auf 10000000 und Betrag auf 0,00 € gesetzt: "
          + satzNr
          + " "
          + day.isoFormat()
          + " "
          + NumberHelper.getAsString(betrag, NumberHelper.getCurrencyFormat(Locale.GERMAN)));
      kost1 = new Double(10000000);
      betrag = BigDecimal.ZERO;
    } else if (kost1 == null || kost2 == null) {
      final String msg = "Oups: Kost1 oder Kost2 ist null für Beleg Nr. " + this.satzNr;
      actionLog.logError(msg);
      log.warn(msg);
    }
  }

  /**
   * Achtung: Diese Klasse ruft ggf. korrigierend und ändernd check() auf.
   * @see java.lang.Object#toString()
   * @see #check()
   */
  public String toString()
  {
    check(); // Leider muss dieser modifizierende check() ausgeführt werden, da auf die aufrufende Klasse ExcelImport kein Einfluss
    // genommen werden kann.
    String txt = StringUtils.abbreviate(text, 30);
    DayHolder day = new DayHolder(datum);
    return StringUtils.leftPad(NumberHelper.getAsString(satzNr), 4)
        + " "
        + StringUtils.leftPad(day.isoFormat(), 10)
        + StringUtils.leftPad(NumberHelper.getAsString(betrag, NumberHelper.getCurrencyFormat(Locale.GERMAN)), 12)
        + " "
        + kost1 != null ? StringUtils.leftPad(kost1.toString(), 12) : "-           " + " " + kost2 != null ? StringUtils.leftPad(kost2
        .toString(), 12) : "-           " + " " + StringUtils.rightPad(txt, 30);
  }

  public void setActionLog(final ActionLog actionLog)
  {
    this.actionLog = actionLog;
  }
}
