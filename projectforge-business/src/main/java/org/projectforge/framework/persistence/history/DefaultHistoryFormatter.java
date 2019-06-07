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

package org.projectforge.framework.persistence.history;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.hibernate.history.delta.PropertyDelta;

public class DefaultHistoryFormatter implements HistoryFormatter
{
  private static final long serialVersionUID = 3232226958091599564L;

  private static final Logger log = LoggerFactory.getLogger(DefaultHistoryFormatter.class);

  private static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  /**
   * Hier werden die keys gespeichert, die nicht gefunden werden konnte. Dies soll ein erneutes Suchen und eine damit
   * verbundene MissingResourceException verhindern. Das Keys nicht gefunden werden, ist normal.
   */
  private Set<String> missingKeys = new HashSet<String>();

  private String resourceBundleName;

  public DefaultHistoryFormatter(String resourceBundleName)
  {
    this.resourceBundleName = resourceBundleName;
  }

  /**
   * Führt toString aus und maskiert alle HTML-Sonderzeichen via StringEscapeUtils.escapeHtml().
   * 
   * @param value Umzuwandelnder Wert.
   * @return Object.toString mit HTML-Escape-Sequenzen oder "", falls value = null.
   */
  public String escapeHtml(Object value)
  {
    return value == null ? "" : StringEscapeUtils.escapeHtml4(String.valueOf(value));
  }

  /**
   * @see de.micromata.hibernate.history.web.HistoryFormatter#getResourceBundle(java.util.Locale)
   */
  @Override
  public ResourceBundle getResourceBundle(final Locale locale)
  {
    return ResourceBundle.getBundle(resourceBundleName, locale, getClass().getClassLoader());
  }

  /**
   * <b>Beachte Sicherheitshinweis:</b> Beim Überladen bitte dynamische Inhalte immer mit escapeHtml ausgeben (Vorsicht:
   * Cross site scripting).
   * 
   * @see de.micromata.hibernate.history.web.HistoryFormatter#formatUser(org.hibernate.Session, java.util.Locale,
   *      de.micromata.hibernate.history.HistoryEntry)
   */
  @Override
  public String formatUser(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta)
  {
    return escapeHtml(historyEntry.getUserName());
  }

  /**
   * @see de.micromata.hibernate.history.web.HistoryFormatter#formatTimestamp(org.hibernate.Session, java.util.Locale,
   *      de.micromata.hibernate.history.HistoryEntry)
   */
  @Override
  public String formatTimestamp(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta)
  {
    return sdf.format(historyEntry.getModifiedAt());
  }

  /**
   * Localized String for action deleted (don't forget escapeHtml!).
   * 
   * @param locale
   */
  public String getDeletedAction(final Locale locale)
  {
    return escapeHtml(getResourceBundle(locale).getString("history.action.deleted"));
  }

  /**
   * Localized String for action updated (don't forget escapeHtml!).
   * 
   * @param locale
   */
  public String getUpdatedAction(final Locale locale)
  {
    return escapeHtml(getResourceBundle(locale).getString("history.action.updated"));
  }

  /**
   * Localized String for action inserted (don't forget escapeHtml!).
   * 
   * @param locale
   */
  public String getInsertedAction(final Locale locale)
  {
    return escapeHtml(getResourceBundle(locale).getString("history.action.inserted"));
  }

  /**
   * @see de.micromata.hibernate.history.web.HistoryFormatter#formatAction(org.hibernate.Session, java.util.Locale,
   *      de.micromata.hibernate.history.HistoryEntry)
   */
  @Override
  public String formatAction(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta)
  {
    switch (historyEntry.getEntityOpType()) {
      case MarkDeleted:
      case Deleted:
        return getDeletedAction(locale);
      case UmarkDeleted:
      case Update:
        return getUpdatedAction(locale);
      case Insert:
        return getInsertedAction(locale);
    }
    return null;
  }

  /**
   * Sucht zuerst im ResourceBundle nach einem Eintrag class.property.xxx (z. B.
   * de.micromata.printing.vw.VWUserDO.property.commitAGBDate), dann nach common.property.xxx und falls beides nicht
   * gefunden wird der Property-Name direkt mit einem Großbuchstaben beginnen ausgegeben. (don't forget escapeHtml!)
   * 
   * @see de.micromata.hibernate.history.web.HistoryFormatter#formatProperty(org.hibernate.Session, java.util.Locale,
   *      java.lang.Object, de.micromata.hibernate.history.HistoryEntry,
   *      de.micromata.hibernate.history.delta.PropertyDelta)
   */
  @Override
  public String formatProperty(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta)
  {
    if (delta == null) {
      return "";
    }
    ResourceBundle resources = getResourceBundle(locale);
    String s = null;
    String key = changed.getClass().getName() + ".property." + delta.getPropertyName();
    if (missingKeys.contains(key) == false) {
      try {
        s = resources.getString(key);
      } catch (MissingResourceException ex) {
        log.debug("Key " + key + " not found (OK, trying next).");
        missingKeys.add(key);
      }
    }
    if (s == null) {
      key = "common.property." + delta.getPropertyName();
      if (missingKeys.contains(key) == false) {
        try {
          s = resources.getString(key);
        } catch (MissingResourceException ex) {
          log.debug("Key " + key + " not found (OK, using default).");
          missingKeys.add(key);
        }
      }
    }
    if (s == null) {
      s = StringUtils.capitalize(delta.getPropertyName());
    }
    return escapeHtml(s);
  }

  /**
   * Wird von formatOldValue und formatNewValue aufgerufen. Dies kann überladen werden, wenn der alte und neue Wert
   * gleichermaßen formatiert werden sollen. Macht hier ersteinmal nichts außer ein escapeHtml(value) aufzurufen.
   * 
   * <b>Beachte Sicherheitshinweis:</b> Beim Überladen bitte dynamische Inhalte immer mit escapeHtml ausgeben (Vorsicht:
   * Cross site scripting).
   * 
   * @param session
   * @param className
   * @param property
   * @param value
   * @return
   */
  public String asString(Session session, final Locale locale, String className, String property, Object value)
  {
    return escapeHtml(value);
  }

  /**
   * (don't forget escapeHtml!)
   * 
   * @see de.micromata.hibernate.history.web.HistoryFormatter#formatOldValue(org.hibernate.Session, java.util.Locale,
   *      java.lang.Object, de.micromata.hibernate.history.HistoryEntry,
   *      de.micromata.hibernate.history.delta.PropertyDelta)
   */
  @Override
  public String formatOldValue(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta)
  {
    return asString(session, locale, historyEntry.getEntityName(), delta == null ? "" : delta.getPropertyName(),
        delta == null ? "" : delta.getOldValue());
  }

  /**
   * (don't forget escapeHtml!)
   * 
   * @see de.micromata.hibernate.history.web.HistoryFormatter#formatNewValue(org.hibernate.Session, java.util.Locale,
   *      java.lang.Object, de.micromata.hibernate.history.HistoryEntry,
   *      de.micromata.hibernate.history.delta.PropertyDelta)
   */
  @Override
  public String formatNewValue(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta)
  {
    return asString(session, locale, historyEntry.getEntityName(), delta == null ? "" : delta.getPropertyName(),
        delta == null ? "" : delta.getNewValue());
  }

  /**
   * @see de.micromata.hibernate.history.web.HistoryFormatter#isVisible(org.hibernate.Session, java.util.Locale,
   *      java.lang.Object, de.micromata.hibernate.history.HistoryEntry,
   *      de.micromata.hibernate.history.delta.PropertyDelta)
   */
  @Override
  public boolean isVisible(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta)
  {
    return true;
  }
}
