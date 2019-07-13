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

package org.projectforge.web;

import org.projectforge.SystemStatus;
import org.projectforge.business.user.UserPrefCache;
import org.projectforge.menu.builder.MenuCreator;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Need by Wicket during the migration phase to Kotlin/Rest, because Wicket/CG-LIB doesn't work properly with
 * SpringBean and Kotlin based Spring components.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

public class WicketSupport {
  private static WicketSupport instance = new WicketSupport();

  /**
   * Workaround for SpringBean and Kotlin Spring components issues.
   *
   * @see WicketSupport
   */
  public static MenuCreator getMenuCreator() {
    return instance.get(MenuCreator.class);
  }

  /**
   * Workaround for SpringBean and Kotlin Spring components issues.
   *
   * @see WicketSupport
   */
  public static UserPrefCache getUserPrefCache() {
    return instance.get(UserPrefCache.class);
  }

  /**
   * Workaround for SpringBean and Kotlin Spring components issues.
   *
   * @see WicketSupport
   */
  public static SystemStatus getSystemStatus() {
    return instance.get(SystemStatus.class);
  }


  public static void register(ApplicationContext applicationContext) {
    // Wicket workaround for not be able to proxy Kotlin base SpringBeans:
    WicketSupport.getInstance().register(applicationContext.getBean(MenuCreator.class));
    WicketSupport.getInstance().register(applicationContext.getBean(UserPrefCache.class));
    WicketSupport.getInstance().register(applicationContext.getBean(SystemStatus.class));
  }

  private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WicketSupport.class);

  private Map<Class<?>, Object> componentsMap = new HashMap<>();

  private WicketSupport() {
  }

  private static WicketSupport getInstance() {
    return instance;
  }

  private void register(Object component) {
    Class<?> clazz = component.getClass();
    register(clazz, component);
  }

  private void register(Class<?> clazz, Object component) {
    if (componentsMap.containsKey(clazz) && componentsMap.get(clazz) != component) {
      log.error("An object for the given clazz " + clazz.getName() + " is already registered and will be overwritten.");
    }
    componentsMap.put(clazz, component);
  }

  private <T> T get(Class<T> clazz) {
    return (T) componentsMap.get(clazz);
  }
}
