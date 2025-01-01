/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu.kost;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.PfCaches;
import org.projectforge.business.fibu.KontoDO;
import org.projectforge.business.fibu.OldKostFormatter;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.PFDayUtils;
import org.projectforge.framework.utils.NumberHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Used in config.xml for the definition of the used business assessment schema. The business assessment is displayed in
 * different accounting areas, such as for DATEV accounting records.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class BusinessAssessment implements Serializable {
  private static final long serialVersionUID = 1752437122374944451L;

  private final BusinessAssessmentConfig config;

  private final List<BusinessAssessmentRow> rows = new ArrayList<>();

  private static final Logger log = LoggerFactory.getLogger(BusinessAssessment.class);

  private String title;

  private String shortname; // Ein Kurzname um z.B. labels oder Dateinamen zu generieren

  private int counter = 0;

  private int year;

  private Integer month;

  private Object reference;

  private boolean storeAccountRecordsInRows;

  /**
   * Fügt alle namentlichen BwaZeilen der Bwa in die übergebene Map. Nützlich für JasperReport, einmal unter der
   * Bezeichnung und einmal unter der Zeilennummer als key.
   *
   * @param map Key ist die Zeilen
   */
  public static void putBusinessAssessmentRows(final Map<String, Object> map,
                                               final BusinessAssessment businessAssessment) {
    for (final BusinessAssessmentRow row : businessAssessment.rows) {
      final double val = getDouble(row.getAmount());
      map.put("r" + row.getNo(), val);
      if (StringUtils.isNotBlank(row.getId())) {
        map.put(row.getId(), val);
      }
    }
  }

  private static double getDouble(final BigDecimal amount) {
    if (amount == null) {
      return 0.0;
    }
    return amount.doubleValue();
  }

  public BusinessAssessment(final BusinessAssessmentConfig config) {
    this.config = config;
    if (config == null || config.getRows() == null) {
      return;
    }
    for (final BusinessAssessmentRowConfig rowConfig : config.getRows()) {
      rows.add(new BusinessAssessmentRow(this, rowConfig));
    }
  }

  public BusinessAssessment(final BusinessAssessmentConfig config, final List<BuchungssatzDO> records) {
    this(config);
    setAccountRecords(records);
  }

  /**
   * @param month 1-January, ..., 12-December.
   */
  public BusinessAssessment(final BusinessAssessmentConfig config, final int year, final Integer month) {
    this(config);
    this.year = year;
    this.month = PFDayUtils.validateMonthValue(month);
  }

  public void setAccountRecords(final List<BuchungssatzDO> records) {
    if (CollectionUtils.isEmpty(rows)) {
      return;
    }
    if (CollectionUtils.isNotEmpty(records)) {
      for (final BuchungssatzDO record : records) {
        counter++;
        // Diese Berechnungen werden anhand des Wertenachweises einer Bwa geführt:
        if (record.isIgnore()) {
          continue;
        }
        if (record.getKonto() == null && record.getGegenKonto() == null) {
          continue;
        }
        boolean found = false;
        for (final BusinessAssessmentRow row : rows) {
          if (doesMatch(row, record)) {
            row.addAccountRecord(record);
            found = true;
            break;
          }
        }
        if (!found) {
          log.warn("Ignoring Satz: " + record);
          record.setIgnore(true);
        }
      }
      recalculate();
    }
  }

  private boolean doesMatch(final BusinessAssessmentRow row, final BuchungssatzDO record) {
    return doesMatch(row, record.getKonto()) || doesMatch(row, record.getGegenKonto());
  }

  private boolean doesMatch(final BusinessAssessmentRow row, final KontoDO account) {
    KontoDO useAccount = PfCaches.getInstance().getKontoIfNotInitialized(account);
    return useAccount != null && useAccount.getNummer() != null && row.doesMatch(useAccount.getNummer());
  }

  public void recalculate() {
    if (rows == null) {
      return;
    }
    for (final BusinessAssessmentRow row : rows) {
      if (row == null) {
        continue;
      }
      row.recalculate();
    }
  }

  /**
   * @return the rows
   */
  public List<BusinessAssessmentRow> getRows() {
    return rows;
  }

  public BusinessAssessmentRow getOverallPerformanceRow() {
    if (config == null) {
      return null;
    }
    return getRow(config.getOverallPerformance());
  }

  public BigDecimal getOverallPerformanceRowAmount() {
    final BusinessAssessmentRow row = getOverallPerformanceRow();
    return row != null ? row.getAmount() : null;
  }

  public BusinessAssessmentRow getMerchandisePurchaseRow() {
    if (config == null) {
      return null;
    }
    return getRow(config.getMerchandisePurchase());
  }

  public BigDecimal getMerchandisePurchaseRowAmount() {
    final BusinessAssessmentRow row = getMerchandisePurchaseRow();
    return row != null ? row.getAmount() : null;
  }

  public BusinessAssessmentRow getPreliminaryResultRow() {
    if (config == null) {
      return null;
    }
    return getRow(config.getPreliminaryResult());
  }

  public BigDecimal getPreliminaryResultRowAmount() {
    final BusinessAssessmentRow row = getPreliminaryResultRow();
    return row != null ? row.getAmount() : null;
  }

  public String asHtml() {
    final StringBuilder buf = new StringBuilder();
    buf.append(getHeader(true));
    buf.append("<table class=\"business-assessment\">\n");
    if (rows != null) {
      for (final BusinessAssessmentRow row : rows) {
        asLine(buf, row, true);
      }
    }
    buf.append("</table>\n");
    return buf.toString();
  }

  public String getHeader() {
    return getHeader(false);
  }

  public String getHeader(final boolean html) {
    final StringBuilder buf = new StringBuilder();
    if (html) {
      buf.append("<h3>");
    }
    if (config != null) {
      buf.append(config.getHeading());
    } else {
      buf.append("business assessment (not defined in config.xml, see AdministrationGuide).");
    }
    if (year > 0) {
      buf.append(": ").append(OldKostFormatter.formatBuchungsmonat(year, month));
    }
    if (title != null) {
      buf.append(" \"").append(title).append("\"");
    }
    if (html) {
      buf.append("</h3>\n");
    } else {
      buf.append(":\n");
    }
    return buf.toString();
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append(getHeader());
    if (rows != null) {
      for (final BusinessAssessmentRow row : rows) {
        asLine(buf, row, false);
      }
    }
    return buf.toString();
  }

  private void asLine(final StringBuilder buf, final String no, final String title, final BigDecimal amount,
                      final int indent,
                      final int scale, final String unit, final boolean html) {
    if (html) {
      buf.append("  <tr><td>").append(no).append("</td><td class=\"indent-").append(indent).append("\">");
    } else {
      buf.append(StringUtils.leftPad(no, 4));
    }
    int length = 25;
    for (int i = 0; i < indent; i++) {
      if (!html) {
        buf.append(" ");
      }
      length--; // One space lost.
    }
    if (html) {
      buf.append(HtmlHelper.escapeHtml(StringUtils.defaultString(title), false)).append("</td>");
    } else {
      buf.append(" ").append(StringUtils.rightPad(StringUtils.defaultString(title), length)).append(" ");
    }
    if (html) {
      buf.append("<td style=\"text-align: right;\">");
    }
    if (amount != null && amount.compareTo(BigDecimal.ZERO) != 0) {
      String value;
      if ("€".equals(unit)) {
        value = CurrencyFormatter.format(amount);
      } else {
        final NumberFormat format = NumberHelper.getNumberFractionFormat(ThreadLocalUserContext.getLocale(), scale);
        value = format.format(amount) + " " + unit;
      }
      buf.append(StringUtils.leftPad(value, 18));
    }
    if (html) {
      buf.append("</td></tr>\n");
    } else {
      buf.append("\n");
    }
  }

  private void asLine(final StringBuilder buf, final BusinessAssessmentRow row, final boolean html) {
    asLine(buf, row.getNo(), row.getTitle(), row.getAmount(), row.getIndent(), row.getScale(), row.getUnit(), html);
  }

  /**
   * @param id id or number of the row.
   * @return The found row or null if not found.
   */
  public BusinessAssessmentRow getRow(final String id) {
    if (rows == null || id == null) {
      return null;
    }
    for (final BusinessAssessmentRow row : rows) {
      if (id.equals(row.getId()) || id.equals(row.getNo())) {
        return row;
      }
    }
    return null;
  }

  public String getShortname() {
    return shortname;
  }

  public void setShortname(final String shortname) {
    this.shortname = shortname;
  }

  public int getCounter() {
    return counter;
  }

  /**
   * Dieses Objekt kann von der benutzenden Klasse als freies Feld genutzt werden. Z. B. wird dieses Feld benutzt, um
   * den Report zu erhalten, der diese BWA enthält
   */
  public Object getReference() {
    return reference;
  }

  public void setReference(final Object reference) {
    this.reference = reference;
  }

  /**
   * @return true if the account records are stored in the rows.
   */
  public boolean isStoreAccountRecordsInRows() {
    return storeAccountRecordsInRows;
  }

  /**
   * @param storeAccountRecordsInRows the storeAccountRecordsInRows to set
   * @return this for chaining.
   */
  public BusinessAssessment setStoreAccountRecordsInRows(final boolean storeAccountRecordsInRows) {
    this.storeAccountRecordsInRows = storeAccountRecordsInRows;
    for (final BusinessAssessmentRow row : this.rows) {
      row.setStoreAccountRecords(this.storeAccountRecordsInRows);
    }
    return this;
  }
}
