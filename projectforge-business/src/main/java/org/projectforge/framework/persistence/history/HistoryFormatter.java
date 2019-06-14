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

import java.io.Serializable;
import java.util.Locale;
import java.util.ResourceBundle;

import org.hibernate.Session;

import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import de.micromata.hibernate.history.delta.PropertyDelta;

/**
 * <b>Beachte Sicherheitshinweis:</b> Beim Überladen bitte dynamische Inhalte immer mit escapeHtml ausgeben (Vorsicht:
 * Cross site scripting).
 */
public interface HistoryFormatter extends Serializable
{
  /**
   * Für die i18n Ausgabe wird das Resource-Bundle benötigt.
   * 
   * @param locale Localization of the required resource bundle
   * @return
   */
  public ResourceBundle getResourceBundle(final Locale locale);

  /**
   * Formatierung des Benutzers, der für die Änderung in der Historie hinterlegt wurde. <b>Beachte
   * Sicherheitshinweis:</b> Beim Überladen bitte dynamische Inhalte immer mit escapeHtml ausgeben (Vorsicht: Cross site
   * scripting).
   * 
   * @param session
   * @param locale
   * @param changed
   * @param historyEntry
   * @param pd
   * @return
   */
  public String formatUser(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta pd);

  /**
   * Formatiert den Zeitstempel, zu dem die Änderung erfolgte.
   * 
   * @param session
   * @param locale
   * @param changed
   * @param historyEntry
   * @param pd
   * @return
   */
  public String formatTimestamp(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta pd);

  /**
   * Formatiert die Art der Änderung (angelegt, geändert, gelöscht).
   * 
   * @param session
   * @param locale
   * @param changed
   * @param historyEntry
   * @param pd
   * @return
   */
  public String formatAction(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta pd);

  /**
   * Formatiert den Feldnamen (Property) dessen Wert geändert wurde.
   * 
   * @param session
   * @param locale
   * @param changed
   * @param historyEntry
   * @param delta
   * @return
   */
  public String formatProperty(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta);

  /**
   * Formatiert den alten Wert der Property, die durch den neuen Wert überschreiben wurde. <b>Beachte
   * Sicherheitshinweis:</b> Beim Überladen bitte dynamische Inhalte immer mit escapeHtml ausgeben (Vorsicht: Cross site
   * scripting).
   * 
   * @param session
   * @param locale
   * @param changed
   * @param historyEntry
   * @param delta
   * @return
   */
  public String formatOldValue(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta);

  /**
   * Formatiert den neuen Wert der Property, der den alten Wert überschrieben hat. <b>Beachte Sicherheitshinweis:</b>
   * Beim Überladen bitte dynamische Inhalte immer mit escapeHtml ausgeben (Vorsicht: Cross site scripting).
   * 
   * @param session
   * @param locale
   * @param changed
   * @param historyEntry
   * @param delta
   * @return
   */
  public String formatNewValue(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta);

  /**
   * Soll die Änderung in der Historie angezeigt werden?
   * 
   * @param session
   * @param locale
   * @param changed
   * @param historyEntry
   * @param delta
   * @return true, wenn die Anzeige erfolgen soll, falls, wenn die Anzeige unterdrückt werden soll.
   */
  public boolean isVisible(Session session, final Locale locale, Object changed, HistoryEntry historyEntry,
      PropertyDelta delta);
}
