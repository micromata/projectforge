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

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;

/**
 * Convert to string for search.
 * 
 * TODO RK will not be loaded, althoug service is registered.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class ProjectForgeDefaultFieldBridge implements BridgeProvider
{
  private static FieldBridge defaultStringBridge = new ToStringFieldBridge();

  @Override
  public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext)
  {

    Class<?> basicClasses[] = {
        //        BigDecimal.class,
        //        Integer.class
    };
    for (Class<?> cls : basicClasses) {
      if (cls.isAssignableFrom(bridgeProviderContext.getReturnType()) == true) {
        return defaultStringBridge;
      }
    }

    return null;
  }

}
