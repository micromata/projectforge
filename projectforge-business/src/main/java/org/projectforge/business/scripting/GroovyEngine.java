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

package org.projectforge.business.scripting;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.common.OutputType;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.ConfigurationServiceAccessor;
import org.projectforge.business.fibu.KostFormatter;
import org.projectforge.business.fibu.KundeDO;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.formatter.TaskFormatter;
import org.projectforge.business.utils.CurrencyFormatter;
import org.projectforge.business.utils.HtmlHelper;
import org.projectforge.common.i18n.I18nEnum;
import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.configuration.Configuration;
import org.projectforge.framework.i18n.I18nHelper;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateTimeFormatter;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyEngine
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GroovyEngine.class);

  private Locale locale;

  private TimeZone timeZone;

  private final boolean htmlFormat = true;

  private final GroovyExecutor groovyExecutor;

  private final Map<String, Object> variables;

  private ConfigurationService configurationService;

  public GroovyEngine(ConfigurationService configurationService, final Locale locale, final TimeZone timeZone)
  {
    this(configurationService, new HashMap<String, Object>(), locale, timeZone);
  }

  public GroovyEngine(ConfigurationService configurationService, final Map<String, Object> variables,
      final Locale locale,
      final TimeZone timeZone)
  {
    if (locale != null) {
      this.locale = locale;
    } else {
      this.locale = ConfigurationServiceAccessor.get().getDefaultLocale();
    }
    if (timeZone != null) {
      this.timeZone = timeZone;
    } else {
      this.timeZone = Configuration.getInstance().getDefaultTimeZone();
    }
    this.variables = variables;
    this.variables.put("pf", this);
    this.groovyExecutor = new GroovyExecutor();
    this.configurationService = configurationService;
  }

  public GroovyEngine(ConfigurationService configurationService, final Map<String, Object> variables)
  {
    this(configurationService, variables, null, null);
  }

  /**
   * For achieving well-formed XML files you can replace '&lt;% ... %&gt;' by '&lt;groovy&gt; ... &lt;/groovy&gt;' and
   * '&lt;%= ... %&gt;' by '&lt;groovy-out&gt; ... &lt;/groovy-out&gt;'
   *
   * @param template
   * @return
   */
  public String preprocessGroovyXml(final String template)
  {
    if (template == null) {
      return null;
    }
    return template.replaceAll("<groovy>", "<% ").replaceAll("</groovy>", " %>").replaceAll("<groovy-out>", "<%= ")
        .replaceAll(
            "</groovy-out>", " %>");
  }

  /**
   * @param variables
   * @see Map#putAll(Map)
   */
  public void putVariables(final Map<String, Object> variables)
  {
    variables.putAll(variables);
  }

  /**
   * @param variables
   * @return this for chaining.
   * @see Map#putAll(Map)
   */
  public GroovyEngine putVariable(final String key, final Object value)
  {
    variables.put(key, value);
    return this;
  }

  /**
   * @param template
   * @see GroovyExecutor#executeTemplate(String, Map)
   */
  public String executeTemplate(final String template)
  {
    final String content = replaceIncludes(template).replaceAll("#HURZ1#", "\\\\").replaceAll("#HURZ2#", "\\$"); // see replaceIncludes
    return groovyExecutor.executeTemplate(content, variables);
  }

  private String replaceIncludes(final String template)
  {
    if (template == null) {
      return null;
    }
    final Pattern p = Pattern.compile("#INCLUDE\\{([0-9\\-\\.a-zA-Z/]*)\\}", Pattern.MULTILINE);
    final StringBuffer buf = new StringBuffer();
    final Matcher m = p.matcher(template);
    while (m.find()) {
      if (m.group(1) != null) {
        final String filename = m.group(1);
        final Object[] res = configurationService.getResourceContentAsString(filename);
        String content = (String) res[0];
        if (content != null) {
          content = replaceIncludes(content).replaceAll("\\\\", "#HURZ1#").replaceAll("\\$", "#HURZ2#");
          m.appendReplacement(buf, content); // Doesn't work with '$' or '\' in content
        } else {
          m.appendReplacement(buf, "*** " + filename + " not found! ***");
        }
      }
    }
    m.appendTail(buf);
    return buf.toString();
  }

  /**
   * @param path
   * @return
   * @see ConfigXml#getResourceContentAsString(String)
   */
  public String executeTemplateFile(final String file)
  {
    final Object[] res = configurationService.getResourceContentAsString(file);
    final String template = (String) res[0];
    if (template == null) {
      log.error("Template with filename '" + file
          + "' not found (whether in resource path nor in ProjectForge's application dir.");
      return "";
    }
    return executeTemplate(template);
  }

  /**
   * Gets i18n message.
   *
   * @param messageKey
   * @param params
   * @see I18nHelper#getLocalizedMessage(Locale, String, Object...)
   */
  public String getMessage(final String messageKey, final Object... params)
  {
    return I18nHelper.getLocalizedMessage(locale, messageKey, params);
  }

  /**
   * Gets i18n string.
   *
   * @param key
   * @see I18nHelper#getLocalizedString(Locale, String)
   */
  public String getI18nString(final String key)
  {
    return I18nHelper.getLocalizedMessage(locale, key);
  }

  /**
   * @param value
   * @return The value as string using the toString() method or "" if the given value is null.
   */
  public String getString(final Object value)
  {
    if (value == null) {
      return "";
    }
    return htmlFormat ? HtmlHelper.formatText(value.toString(), true) : value.toString();
  }

  /**
   * @param value
   * @return true if the value is null or instance of NullObject, otherwise false.
   */
  public boolean isNull(final Object value)
  {
    if (value == null) {
      return true;
    }
    return value instanceof NullObject;
  }

  /**
   * @param value
   * @see StringUtils#isBlank(String)
   */
  public boolean isBlank(final String value)
  {
    return StringUtils.isBlank(value);
  }

  /**
   * @param value
   * @return true if value is null or value.toString() is blank.
   * @see StringUtils#isBlank(String)
   */
  public boolean isBlank(final Object value)
  {
    if (value == null) {
      return true;
    }
    return StringUtils.isBlank(value.toString());
  }

  /**
   * @param value
   * @return Always true.
   */
  public boolean isBlank(final NullObject value)
  {
    return true;
  }

  /**
   * @param value
   * @see StringUtils#isEmpty(String)
   */
  public boolean isEmpty(final String value)
  {
    return StringUtils.isEmpty(value);
  }

  /**
   * @param value
   * @return true if value is null or value.toString() is empty.
   * @see StringUtils#isEmpty(String)
   */
  public boolean isEmpty(final Object value)
  {
    if (value == null) {
      return true;
    }
    return StringUtils.isEmpty(value.toString());
  }

  /**
   * @param value
   * @return Always true.
   */
  public boolean isEmpty(final NullObject value)
  {
    return true;
  }

  public void log(final String message)
  {
    log.info(message);
  }

  /**
   * @param value
   * @return The given string itself or "" if value is null.
   */
  public String getString(final String value)
  {
    if (value == null) {
      return "";
    }
    return htmlFormat ? HtmlHelper.formatText(value, true) : value;
  }

  /**
   * Gets i18n string.
   *
   * @param i18nEnum
   * @see I18nHelper#getLocalizedString(Locale, String)
   * @see I18nEnum#getI18nKey()
   */
  public String getString(final I18nEnum i18nEnum)
  {
    if (i18nEnum == null) {
      return "";
    }
    return I18nHelper.getLocalizedMessage(locale, i18nEnum.getI18nKey());
  }

  /**
   * Gets the customer's name.
   *
   * @param customer
   * @see KostFormatter#formatKunde(KundeDO)
   */
  public String getString(final KundeDO customer)
  {
    if (customer == null) {
      return "";
    }
    return KostFormatter.formatKunde(customer);
  }

  /**
   * Gets the project's name.
   *
   * @param project
   * @see KostFormatter#formatProjekt(ProjektDO)
   */
  public String getString(final ProjektDO project)
  {
    if (project == null) {
      return "";
    }
    return KostFormatter.formatProjekt(project);
  }

  /**
   * Gets the user's name (full name).
   *
   * @param user
   * @see PFUserDO#getFullname()
   */
  public String getString(final PFUserDO user)
  {
    if (user == null) {
      return "";
    }
    return user.getFullname();
  }

  /**
   * Gets the user's name (full name).
   *
   * @param user
   * @see PFUserDO#getFullname()
   */
  public String getString(final Number value)
  {
    if (value == null) {
      return "";
    }
    return NumberHelper.getAsString(value);
  }

  /**
   * Gets the user's name (full name).
   *
   * @param user
   * @see PFUserDO#getFullname()
   */
  public String getString(final TaskDO task)
  {
    if (task == null) {
      return "";
    }
    return TaskFormatter.getTaskPath(task.getId(), true, OutputType.PLAIN);
  }

  public String getCurrency(final BigDecimal value)
  {
    if (value == null) {
      return "";
    }
    return CurrencyFormatter.format(value, locale);
  }

  public String getString(final BigDecimal value)
  {
    if (value == null) {
      return "";
    }
    return NumberFormatter.format(value, locale);
  }

  public String getString(final java.sql.Date date)
  {
    if (date == null) {
      return "";
    }
    return DateTimeFormatter.instance().getFormattedDate(date, locale, timeZone);
  }

  public String getString(final java.util.Date date)
  {
    if (date == null) {
      return "";
    }
    return DateTimeFormatter.instance().getFormattedDateTime(date, locale, timeZone);
  }
}
