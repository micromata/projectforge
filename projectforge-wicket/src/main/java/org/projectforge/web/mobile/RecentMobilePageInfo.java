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

package org.projectforge.web.mobile;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Stores the recent called mobile page including it's parameters. So after re-login through the mobile client the user is redirected to his
 * recent page.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RecentMobilePageInfo implements Serializable
{
  private static final long serialVersionUID = -7269142360684935021L;

  private Class< ? extends AbstractSecuredMobilePage> pageClass;

  private Map<String, Object> pageParameters;

  public RecentMobilePageInfo()
  {
  }

  public RecentMobilePageInfo(final AbstractSecuredMobilePage page)
  {
    if (page == null) {
      return;
    }
    this.pageClass = page.getClass();
    if (page.getPageParameters() != null) {
      pageParameters = new HashMap<String, Object>();
      for (final NamedPair entry : page.getPageParameters().getAllNamed()) {
        final Object value = entry.getValue();
        if (value == null) {
          continue;
        }
        if (value instanceof Integer || value instanceof String) {
          pageParameters.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  public Class< ? extends AbstractSecuredMobilePage> getPageClass()
  {
    return pageClass;
  }

  public PageParameters restorePageParameters()
  {
    if (pageParameters == null) {
      return null;
    }
    final PageParameters params = new PageParameters();
    for (final Entry<String, Object> entry : pageParameters.entrySet()) {
      params.add(entry.getKey(), entry.getValue());
    }
    return params;
  }
}
