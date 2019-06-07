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

package org.projectforge.framework.configuration;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Holds the application context for implementation, which has no possibilities to be registered itself as spring bean..
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware
{
  private static ApplicationContext applicationContext;

  public static ApplicationContext getApplicationContext()
  {
    return applicationContext;
  }

  @Override
  public void setApplicationContext(ApplicationContext ctx) throws BeansException
  {
    applicationContext = ctx;
  }
}
