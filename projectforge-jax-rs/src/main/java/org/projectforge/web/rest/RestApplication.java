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

package org.projectforge.web.rest;

import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Initial servlet for initiating the restful services.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Controller
@ApplicationPath("/")
public class RestApplication extends Application
{

  @Autowired
  RestCallRegistry restCallRegistry;

  /**
   * @return all restful service classes.
   * @see javax.ws.rs.core.Application#getClasses()
   */
  @Override
  public Set<Class<?>> getClasses()
  {
    return restCallRegistry.getClasses();
  }
}
