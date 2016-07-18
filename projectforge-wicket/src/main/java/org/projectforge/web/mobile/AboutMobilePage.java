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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.AppVersion;
import org.projectforge.ProjectForgeVersion;
import org.projectforge.web.wicket.WicketUtils;

public class AboutMobilePage extends AbstractMobilePage
{
  private static final long serialVersionUID = -3508519141413660648L;

  public AboutMobilePage()
  {
    this(new PageParameters());
  }

  public AboutMobilePage(final PageParameters parameters)
  {
    super(parameters);
    pageContainer.add(new Label("text", getString("mobile.about.text")).setEscapeModelStrings(false));
    pageContainer.add(new Label("copyright", "©2001-" + ProjectForgeVersion.YEAR + " by Kai Reinhard, Micromata GmbH, Germany"));
  }

  @Override
  protected String getTitle()
  {
    return getString("mobile.about");
  }

  /**
   * @return Home link as default.
   */
  @Override
  protected void addTopCenter()
  {
    headerContainer.add(new Label(AbstractMobilePage.TOP_CENTER_ID, AppVersion.APP_TITLE));
  }

  /**
   * Invisible
   */
  @Override
  protected void addTopRightButton()
  {
    headerContainer.add(WicketUtils.getInvisibleComponent(TOP_RIGHT_BUTTON_ID));
  }

  /**
   * @see org.projectforge.web.mobile.AbstractMobilePage#thisIsAnUnsecuredPage()
   */
  @Override
  protected void thisIsAnUnsecuredPage()
  {
  }
}
