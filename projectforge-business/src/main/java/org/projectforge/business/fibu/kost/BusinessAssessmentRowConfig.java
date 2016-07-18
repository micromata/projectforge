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

package org.projectforge.business.fibu.kost;

import groovy.lang.Script;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.projectforge.business.scripting.GroovyExecutor;
import org.projectforge.common.i18n.Priority;
import org.projectforge.framework.utils.IntRanges;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;
import org.projectforge.framework.xstream.XmlOmitField;

/**
 * Used in config.xml for the definition of the used business assessment schema. This object represents a single row of the business
 * assessment.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "row")
public class BusinessAssessmentRowConfig implements Serializable
{
  private static final long serialVersionUID = -8441226359103634737L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BusinessAssessmentRowConfig.class);

  // <row no="1051" id="gesamtleistung" value="umsatzErloese+bestVerdg+aktEigenleistungen" priority="high" title="Gesamtleistung" />

  @XmlOmitField
  IntRanges accountNumberRanges;

  @XmlField(asAttribute = true)
  private String no;

  @XmlField(asAttribute = true)
  private String id;

  @XmlField(asAttribute = true, alias = "accountRange")
  private String accountRangeConfig;

  @XmlField(alias = "value")
  private String valueConfig;

  private transient Script valueScript;

  private Priority priority;

  @XmlField(asAttribute = true)
  private String title;

  private int indent, scale = -1;

  private String unit;

  @XmlOmitField
  private boolean initialized;

  public BusinessAssessmentRowConfig()
  {
  }

  /**
   * The number has no other functionality than to be displayed.
   * @return Number to display (e. g. 1051).
   */
  public String getNo()
  {
    return no;
  }

  /**
   * The id can be used for referring the row e. g. inside scripts or for calculating values (see {@link #getValue()}).
   * @see #getValue()
   */
  public String getId()
  {
    return id;
  }

  /**
   * The amount is calculated by adding all account records of the given account range. The account range is a coma separated list of
   * accounts and account ranges (DATEV accounts) such as "4830,4947", "4000-4799" or "6300,6800-6855".
   * @return the accountRange
   */
  public String getAccountRangeConfig()
  {
    return accountRangeConfig;
  }

  /**
   * The value is optional and used if the amount of this row has to be calculated. If the string starts with '=' then the value is
   * calculated, e. g. "= resultBeforeTaxes + taxesAndOtherIncomes". resultBeforeTaxes and taxesAndOtherIncomes are id's of rows available
   * as variables. <br/>
   * If the string doesn't start with a '=' the value will be taken as Groovy script and the returned value of this script is taken as
   * amount of this row.
   */
  public String getValueConfig()
  {
    return valueConfig;
  }

  /**
   * @return the valueScript
   */
  Script getValueScript()
  {
    if (valueScript == null && StringUtils.isBlank(this.valueConfig) == false) {
      String scr = this.valueConfig.trim();
      if (scr.startsWith("=") == true) {
        scr = "return " + scr.substring(1);
      }
      this.valueScript = new GroovyExecutor().compileGroovy(scr, false);
    }
    return valueScript;
  }

  /**
   * Priority to display. If a short business assessment is displayed only rows with high priority are shown.
   * @return
   */
  public Priority getPriority()
  {
    return priority;
  }

  /**
   * The title will be displayed.
   */
  public String getTitle()
  {
    return title;
  }

  /**
   * Only for indenting when displaying this row.
   * @return the indent
   */
  public int getIndent()
  {
    return indent;
  }

  /**
   * If not given the standard scale 2 is used.
   * @return the scale
   */
  public int getScale()
  {
    return scale;
  }

  public void setScale(final int scale)
  {
    this.scale = scale;
  }

  /**
   * @return the unit
   */
  public String getUnit()
  {
    return unit;
  }

  /**
   * @return the accountNumberRanges
   */
  public IntRanges getAccountNumberRanges()
  {
    initialize();
    return accountNumberRanges;
  }

  @Override
  public String toString()
  {
    final ReflectionToStringBuilder builder = new ReflectionToStringBuilder(this);
    return builder.toString();
  }

  /**
   * Extract the account ranges of the configured accountRage at set the ranges. Examples: "4830,4947", "4000-4799" or "6300,6800-6855"
   */
  private synchronized void initialize()
  {
    if (initialized == true) {
      return;
    }
    try {
      accountNumberRanges = new IntRanges(accountRangeConfig);
    } catch (final Exception ex) {
      log.warn("Couldn't parse number range of businessAssessmentRow '" + accountRangeConfig + "':" + ex.getMessage(), ex);
    }
    initialized = true;
  }
}
