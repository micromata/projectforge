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

package org.projectforge.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.projectforge.framework.configuration.ConfigXml;
import org.projectforge.framework.xstream.XmlField;
import org.projectforge.framework.xstream.XmlObject;
import org.projectforge.framework.xstream.XmlOmitField;
import org.projectforge.menu.builder.MenuItemDef;

/**
 * Defining a new menu entry or modify the existing tree by config.xml or configures an existing menu entry as invisible. Any access
 * settings are not yet available (planned if really needed by the community).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@XmlObject(alias = "menu-entry")
public class MenuEntryConfig
{
  @XmlField(asAttribute = true)
  private String url;

  @XmlField(alias = "id")
  private String menuItemId;

  @XmlField(asAttribute = true)
  private String label;

  private List<Translation> translations;

  @XmlField(defaultBooleanValue = true)
  private boolean visible = true;

  @XmlField(alias = "sub-menu")
  private List<MenuEntryConfig> children;

  @XmlOmitField
  private MenuEntryConfig parent;

  /**
   * @return The MenuItemDef if this menu entry is one of the ProjectForge pre-defined menu entries, otherwise null.
   */
  public String getMenuItemId()
  {
    return menuItemId;
  }

  public String getUrl()
  {
    return url;
  }

  /**
   * This label is the default label if no label for the user's language is found in the list of translations.
   */
  public String getLabel()
  {
    return label;
  }

  MenuEntryConfig setLabel(final String label)
  {
    this.label = label;
    return this;
  }

  public String getLabel(final Locale locale)
  {
    if (locale == null || translations == null) {
      return getLabel();
    }
    final Translation translation = getTranslation(locale);
    if (translation != null) {
      return translation.getTranslation();
    }
    return getLabel();
  }

  public Translation getTranslation(final Locale locale)
  {
    if (locale == null || translations == null) {
      return null;
    }
    String lang = locale.toString().toLowerCase();
    for (int i = 0; i < 4; i++) { // Endless loop protection.
      final Translation translation = getTranslation(lang);
      if (translation != null) {
        return translation;
      }
      final int pos = lang.lastIndexOf('_');
      if (pos > 0) {
        lang = lang.substring(0, pos);
      } else {
        break;
      }
    }
    return null;
  }

  public Translation getTranslation(final String lang)
  {
    if (lang == null || translations == null) {
      return null;
    }
    for (final Translation translation : translations) {
      if (lang.toLowerCase().equals(translation.getLocale().toString().toLowerCase()) == true) {
        return translation;
      }
    }
    return null;
  }

  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Needed but not used, otherwise code reformatter transforms member visible to final member.
   * @param visible the visible to set
   */
  void setVisible(final boolean visible)
  {
    this.visible = visible;
  }

  public List<MenuEntryConfig> getChildren()
  {
    return children;
  }

  /**
   * Find a pre-defined ProjectForge menu entry.
   */
  public MenuEntryConfig findMenuEntry(final MenuItemDef menuItemDef)
  {
    if (menuItemDef != null && menuItemDef.getId() != null && menuItemDef.getId().equals(menuItemId) == true) {
      return this;
    }
    if (children == null) {
      return null;
    }
    for (final MenuEntryConfig child : children) {
      final MenuEntryConfig foundEntry = child.findMenuEntry(menuItemDef);
      if (foundEntry != null) {
        return foundEntry;
      }
    }
    return null;
  }

  public void setParent(final MenuEntryConfig parent)
  {
    this.parent = parent;
  }

  /**
   * Ensure that you have called setParents on the root element before, otherwise the parent field is always null.
   */
  public MenuEntryConfig getParent()
  {
    return parent;
  }

  /**
   * Walks through the tree and sets the parent menu entry for each descendant.
   */
  public void setParents()
  {
    if (children == null) {
      return;
    }
    for (final MenuEntryConfig child : children) {
      child.setParent(this);
      child.setParents();
    }
  }

  /**
   * Label in the different languages.
   */
  public List<Translation> getTranslations()
  {
    return translations;
  }

  MenuEntryConfig addTranslation(final Translation translation)
  {
    if (translations == null) {
      translations = new ArrayList<Translation>();
    }
    translations.add(translation);
    return this;
  }

  MenuEntryConfig addTranslation(final Locale locale, final String translation)
  {
    return addTranslation(new Translation(locale, translation));
  }

  MenuEntryConfig addTranslation(final String locale, final String translation)
  {
    return addTranslation(new Locale(locale), translation);
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
