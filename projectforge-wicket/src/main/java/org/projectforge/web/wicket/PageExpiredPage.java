/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.wicket;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Standard error page should be shown in production mode. Redirect for mobile user agents.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class PageExpiredPage extends MessagePage
{
  public PageExpiredPage(final PageParameters params)
  {
    super(params);
    setMessage(getString("message.wicket.pageExpired"));
  }
}
