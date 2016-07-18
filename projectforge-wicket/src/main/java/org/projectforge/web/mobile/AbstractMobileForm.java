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

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.mobileflowlayout.MobileGridBuilder;

public abstract class AbstractMobileForm<F, P extends AbstractMobilePage> extends Form<F>
{
  private static final long serialVersionUID = 3798448024275972658L;

  protected final P parentPage;

  public AbstractMobileForm(final P parentPage)
  {
    super("form");
    this.parentPage = parentPage;
  }

  public MySession getMySession()
  {
    return (MySession) getSession();
  }

  /**
   * @see GridBuilder#GridBuilder(RepeatingView, MySession)
   */
  protected MobileGridBuilder newGridBuilder(final RepeatingView parent)
  {
    return new MobileGridBuilder(parent);
  }

  /**
   * @see GridBuilder#GridBuilder(DivPanel, MySession)
   */
  protected MobileGridBuilder newGridBuilder(final DivPanel parent)
  {
    return new MobileGridBuilder(parent);
  }
}
