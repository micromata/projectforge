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

import java.io.Serializable;

import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.utils.IntRanges;
import org.projectforge.framework.xstream.AliasMap;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;
import org.projectforge.framework.xstream.XmlObjectReader;

/**
 * Used in config.xml for the definition of the used business assessment schema. The business assessment is displayed in different
 * accounting areas, such as for DATEV accounting records.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "accounting")
public class AccountingConfig implements Serializable
{
  private static final long serialVersionUID = 21261507180352913L;

  private BusinessAssessmentConfig businessAssessment;

  @XmlField(alias = "debitorsAccountNumberRanges")
  private String debitorsAccountNumberRangesString;

  @XmlField(alias = "creditorsAccountNumberRanges")
  private String creditorsAccountNumberRangesString;

  /**
   * Gets the default configuration from config.xml.
   */
  public static AccountingConfig getInstance()
  {
    return ConfigXml.getInstance().getAccountingConfig();
  }

  public static void registerXmlObjects(final XmlObjectReader reader, final AliasMap aliasMap)
  {
    aliasMap.put(BusinessAssessmentRowConfig.class, "row");
    reader.initialize(BusinessAssessmentConfig.class);
  }

  /**
   * The number ranges of the account numbers for the debitors, e. g. "10000-12999".
   * @return the debitorsAccountNumberRanges
   */
  public IntRanges getDebitorsAccountNumberRanges()
  {
    return (IntRanges) new IntRanges(debitorsAccountNumberRangesString).setNullRangeMatchesAlways(true);
  }

  /**
   * The number ranges of the account numbers for the creditors, e. g. "10000-12999".
   * @return the creditorsAccountNumberRanges
   */
  public IntRanges getCreditorsAccountNumberRanges()
  {
    return (IntRanges) new IntRanges(creditorsAccountNumberRangesString).setNullRangeMatchesAlways(true);
  }

  public void reset()
  {
    businessAssessment = null;
  }

  /**
   * @return the businessAssessmentConfig
   */
  public BusinessAssessmentConfig getBusinessAssessmentConfig()
  {
    return businessAssessment;
  }

  /**
   * @see ConfigXml#toString(Object)
   */
  @Override
  public String toString()
  {
    return ConfigXml.toString(this);
  }
}
