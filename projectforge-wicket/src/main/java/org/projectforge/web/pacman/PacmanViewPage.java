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

package org.projectforge.web.pacman;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.web.wicket.AbstractSecuredPage;

/**
 * https://github.com/daleharvey/pacman
 */
public class PacmanViewPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 6317381238021216284L;

  public PacmanViewPage(final PageParameters parameters)
  {
    this(parameters, null);
  }

  public PacmanViewPage(final PageParameters parameters, final AbstractSecuredPage returnToPage)
  {
    super(parameters);
  }

  @Override
  protected String getTitle()
  {
    return getString("pacman.title");
  }

}