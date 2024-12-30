/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.common.OutputType
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.OldKostFormatter
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskFormatter
import org.projectforge.business.utils.CurrencyFormatter
import org.projectforge.business.utils.HtmlHelper
import org.projectforge.common.FormatterUtils
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.i18n.I18nHelper.getLocalizedMessage
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.utils.NumberFormatter.format
import org.projectforge.framework.utils.NumberHelper.getAsString
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import java.util.regex.Pattern

private val log = KotlinLogging.logger {}

class GroovyEngine @JvmOverloads constructor(
  val configurationService: ConfigurationService,
  var variables: MutableMap<String, Any?>,
  locale: Locale? = null,
  timeZone: TimeZone? = null
) {

  @JvmOverloads
  constructor(
    configurationService: ConfigurationService,
    locale: Locale? = null,
    timeZone: TimeZone? = null
  )
      : this(configurationService, mutableMapOf(), locale, timeZone)

  private val locale = locale ?: ThreadLocalUserContext.locale!!
  private val timeZone = timeZone ?: Configuration.instance.defaultTimeZone
  private val htmlFormat = true
  private val groovyExecutor = GroovyExecutor()

  init {
    this.variables["pf"] = this
  }

  /**
   * For achieving well-formed XML files you can replace '&lt;% ... %&gt;' by '&lt;groovy&gt; ... &lt;/groovy&gt;' and
   * '&lt;%= ... %&gt;' by '&lt;groovy-out&gt; ... &lt;/groovy-out&gt;'
   *
   * @param template
   * @return
   */
  fun preprocessGroovyXml(template: String?): String? {
    return template?.replace("<groovy>".toRegex(), "<% ")?.replace("</groovy>".toRegex(), " %>")
      ?.replace("<groovy-out>".toRegex(), "<%= ")?.replace(
      "</groovy-out>".toRegex(), " %>"
    )
  }

  /**
   * @param variables
   */
  fun putVariables(variables: Map<String, Any>) {
    this.variables.putAll(variables)
  }

  /**
   * @return this for chaining.
   */
  fun putVariable(key: String, value: Any): GroovyEngine {
    variables[key] = value
    return this
  }

  fun isVariableGiven(key: String): Boolean {
    val value = variables[key] ?: return false
    if (value is String) {
      return value.isNotEmpty()
    }
    return true
  }

  /**
   * @param template
   * @see GroovyExecutor.executeTemplate
   */
  fun executeTemplate(template: String): String {
    val content = replaceIncludes(template)!!.replace("#HURZ1#".toRegex(), "\\\\")
      .replace("#HURZ2#".toRegex(), "\\$") // see replaceIncludes
    return groovyExecutor.executeTemplate(content, variables)
  }

  private fun replaceIncludes(template: String?): String? {
    if (template == null) {
      return null
    }
    val p = Pattern.compile("#INCLUDE\\{([0-9\\-\\.a-zA-Z/]*)\\}", Pattern.MULTILINE)
    val buf = StringBuilder()
    val m = p.matcher(template)
    while (m.find()) {
      if (m.group(1) != null) {
        val filename = m.group(1)
        val res = configurationService.getResourceContentAsString(filename)
        var content = res[0] as? String
        if (content != null) {
          content = replaceIncludes(content)!!.replace("\\\\".toRegex(), "#HURZ1#").replace("\\$".toRegex(), "#HURZ2#")
          m.appendReplacement(buf, content) // Doesn't work with '$' or '\' in content
        } else {
          m.appendReplacement(buf, "*** $filename not found! ***")
        }
      }
    }
    m.appendTail(buf)
    return buf.toString()
  }

  /**
   * @see ConfigurationService.getResourceContentAsString
   */
  fun executeTemplateFile(file: String): String {
    val res = configurationService.getResourceContentAsString(file)
    val template = res[0] as? String
    if (template == null) {
      log.error(
        "Template with filename '" + file
            + "' not found (neither in resource path nor in ProjectForge's application dir."
      )
      return ""
    }
    return executeTemplate(template)
  }

  /**
   * Gets i18n message.
   *
   * @param messageKey
   * @param params
   * @see I18nHelper.getLocalizedMessage
   */
  fun getMessage(messageKey: String?, vararg params: Any?): String {
    return getLocalizedMessage(locale, messageKey, *params)
  }

  /**
   * Gets i18n string.
   *
   * @param key
   * @see I18nHelper.getLocalizedString
   */
  fun getI18nString(key: String?): String {
    val str = getLocalizedMessage(locale, key)
    return if (htmlFormat) HtmlHelper.formatText(str, true) else str
  }

  /**
   * @param value
   * @return The value as string using the toString() method or "" if the given value is null.
   */
  fun getString(value: Any?): String {
    if (value == null) {
      return ""
    }
    return if (htmlFormat) HtmlHelper.formatText(value.toString(), true) else value.toString()
  }

  /**
   * @param value
   * @return true if the value is null or instance of NullObject, otherwise false.
   */
  fun isNull(value: Any?): Boolean {
    return if (value == null) {
      true
    } else value is NullObject
  }

  /**
   * @param value
   * @see StringUtils.isBlank
   */
  fun isBlank(value: String?): Boolean {
    return StringUtils.isBlank(value)
  }

  /**
   * @param value
   * @return true if value is null or value.toString() is blank.
   * @see StringUtils.isBlank
   */
  fun isBlank(value: Any?): Boolean {
    return if (value == null) {
      true
    } else StringUtils.isBlank(value.toString())
  }

  /**
   * @param value
   * @return Always true.
   */
  @Suppress("UNUSED_PARAMETER")
  fun isBlank(value: NullObject?): Boolean {
    return true
  }

  /**
   * @param value
   * @see StringUtils.isEmpty
   */
  fun isEmpty(value: String?): Boolean {
    return StringUtils.isEmpty(value)
  }

  /**
   * @param value
   * @return true if value is null or value.toString() is empty.
   * @see StringUtils.isEmpty
   */
  fun isEmpty(value: Any?): Boolean {
    return if (value == null) {
      true
    } else StringUtils.isEmpty(value.toString())
  }

  /**
   * @param value
   * @return Always true.
   */
  @Suppress("UNUSED_PARAMETER")
  fun isEmpty(value: NullObject?): Boolean {
    return true
  }

  fun log(message: String?) {
    log.info(message)
  }

  /**
   * @param value
   * @return The given string itself or "" if value is null.
   */
  fun getString(value: String?): String {
    if (value == null) {
      return ""
    }
    return if (htmlFormat) HtmlHelper.formatText(value, true) else value
  }

  /**
   * Gets i18n string.
   *
   * @param i18nEnum
   * @see I18nHelper.getLocalizedString
   * @see I18nEnum.getI18nKey
   */
  fun getString(i18nEnum: I18nEnum?): String {
    return if (i18nEnum == null) {
      ""
    } else getLocalizedMessage(locale, i18nEnum.i18nKey)
  }

  /**
   * Gets the customer's name.
   *
   * @param customer
   * @see OldKostFormatter.formatKunde
   */
  fun getString(customer: KundeDO?): String {
    return if (customer == null) {
      ""
    } else OldKostFormatter.formatKunde(customer)
  }

  /**
   * Gets the project's name.
   *
   * @param project
   * @see OldKostFormatter.formatProjekt
   */
  fun getString(project: ProjektDO?): String {
    return if (project == null) {
      ""
    } else OldKostFormatter.formatProjekt(project)
  }

  /**
   * Gets the user's name (full name).
   *
   * @param user
   * @see PFUserDO.getFullname
   */
  fun getString(user: PFUserDO?): String {
    return user?.getFullname() ?: ""
  }

  fun getString(value: Number?): String {
    return value?.let { getAsString(it) } ?: ""
  }

  fun getString(task: TaskDO?): String {
    return if (task == null) {
      ""
    } else TaskFormatter.getTaskPath(task.id, true, OutputType.PLAIN) ?: ""
  }

  fun getCurrency(value: BigDecimal?): String {
    return if (value == null) {
      ""
    } else CurrencyFormatter.format(value, locale)
  }

  fun getString(value: BigDecimal?): String {
    return if (value == null) {
      ""
    } else format(value, locale)
  }

  fun getString(date: LocalDate?): String {
    return if (date == null) {
      ""
    } else DateTimeFormatter.instance().getFormattedDate(date, locale, timeZone)
  }

  fun getString(date: Date?): String {
    return if (date == null) {
      ""
    } else DateTimeFormatter.instance().getFormattedDateTime(date, locale, timeZone)
  }

  fun formatBytes(bytes: Long?): String {
    return FormatterUtils.formatBytes(bytes, locale)
  }
}
