/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.projectforge.framework.configuration.ConfigurationJsonValues;
import org.projectforge.framework.configuration.ConfigurationParam;
import org.projectforge.framework.configuration.SecretFieldToString;
import org.projectforge.framework.json.JsonUtils;
import org.projectforge.framework.utils.IntRanges;
import org.projectforge.framework.xmlstream.AliasMap;
import org.projectforge.framework.xmlstream.XmlField;
import org.projectforge.framework.xmlstream.XmlObject;
import org.projectforge.framework.xmlstream.XmlObjectReader;

import java.io.Serializable;

/**
 * The accounting settings: the account number ranges of debitors and creditors and the used business assessment
 * schema. The business assessment is displayed in different accounting areas, such as for DATEV accounting records.
 * Stored as JSON in the configuration parameter fibu.accounting, the xml annotations are only used for reading the
 * former config.xml.
 *
 * @author Kai Reinhard
 *
 */
@XmlObject(alias = "accounting")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountingConfig implements Serializable
{
  private static final long serialVersionUID = 21261507180352913L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountingConfig.class);

  private static String cachedJson;

  private static AccountingConfig cachedInstance;

  private BusinessAssessmentConfig businessAssessment;

  @XmlField(alias = "debitorsAccountNumberRanges")
  @JsonProperty("debitorsAccountNumberRanges")
  private String debitorsAccountNumberRangesString;

  @XmlField(alias = "creditorsAccountNumberRanges")
  @JsonProperty("creditorsAccountNumberRanges")
  private String creditorsAccountNumberRangesString;

  /**
   * Gets the configuration of the parameter fibu.accounting. It's cached as long as the parameter isn't changed, so
   * the scripts of the business assessment rows are compiled once.
   */
  public static synchronized AccountingConfig getInstance()
  {
    final String json = ConfigurationJsonValues.get(ConfigurationParam.FIBU_ACCOUNTING);
    if (cachedInstance == null || !java.util.Objects.equals(json, cachedJson)) {
      cachedInstance = parse(json);
      cachedJson = json;
    }
    return cachedInstance;
  }

  /**
   * @return The configuration of the given JSON value, an empty one if blank or not readable.
   */
  public static AccountingConfig parse(final String json)
  {
    if (json == null || json.isBlank()) {
      return new AccountingConfig();
    }
    try {
      final AccountingConfig config = JsonUtils.fromJson(json, AccountingConfig.class, false);
      return config != null ? config : new AccountingConfig();
    } catch (final Exception ex) {
      log.warn("Accounting: configuration (fibu.accounting) not readable, using an empty one: " + ex.getMessage());
      return new AccountingConfig();
    }
  }

  public static void registerXmlObjects(final XmlObjectReader reader, final AliasMap aliasMap)
  {
    aliasMap.put(BusinessAssessmentRowConfig.class, "row");
    reader.initialize(BusinessAssessmentConfig.class);
  }

  /**
   * @return The configured number ranges of the account numbers for the debitors as given, e. g. "10000-12999".
   */
  public String getDebitorsAccountNumberRangesString()
  {
    return debitorsAccountNumberRangesString;
  }

  /**
   * @return The configured number ranges of the account numbers for the creditors as given, e. g. "70000-79999".
   */
  public String getCreditorsAccountNumberRangesString()
  {
    return creditorsAccountNumberRangesString;
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

  /**
   * @return the businessAssessmentConfig
   */
  public BusinessAssessmentConfig getBusinessAssessmentConfig()
  {
    return businessAssessment;
  }

  /**
   * @see SecretFieldToString#toString(Object)
   */
  @Override
  public String toString()
  {
    return SecretFieldToString.toString(this);
  }
}
