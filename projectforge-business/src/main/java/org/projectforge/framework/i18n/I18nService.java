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

package org.projectforge.framework.i18n;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by mhesse on 24.03.16.
 */
public interface I18nService
{
  void loadResourceBundles();

  String getLocalizedStringForKey(String i18nKey, Locale locale);

  ResourceBundle getResourceBundleFor(String name, Locale locale);

  String getAdditionalString(String key, Locale locale);

  /**
   * The keys and values of the customer-specific {@code CustomerI18nResources} bundle for the given locale,
   * or an empty map if this deployment ships no such bundle. These are the deployment's overrides — the
   * texts a customer changed without touching the code (see {@code I18nHelper.addBundleNameWithHighestPriority}).
   * projectforge-next ships a static message catalog built from the product bundle only, so it fetches these
   * at runtime and overlays them with highest priority, the way the server-rendered UILayout pages did through
   * {@code I18nHelper} automatically.
   */
  Map<String, String> getCustomerI18nOverrides(Locale locale);
}
