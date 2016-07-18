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

package org.projectforge.web.wicket;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public abstract class AbstractMassEditPage extends AbstractSecuredPage
{
  private static final long serialVersionUID = 8283877351980165438L;

  protected final AbstractSecuredPage callerPage;

  public AbstractMassEditPage(final PageParameters parameters, final AbstractSecuredPage callerPage)
  {
    super(parameters);
    this.callerPage = callerPage;
    body.add(new Label("showUpdateQuestionDialog", "function showUpdateQuestionDialog() {\n" + //
        "  return window.confirm('"
        + getString("question.massUpdateQuestion")
        + "');\n"
        + "}\n") //
    .setEscapeModelStrings(false));
  }

  protected void cancel()
  {
    setResponsePage(callerPage);
  }

  /**
   * User has clicked the update button for updating an existing item.
   */
  protected void updateAll()
  {
    if (callerPage instanceof AbstractListPage< ? , ? , ? >) {
      ((AbstractListPage< ? , ? , ? >) callerPage).setMassUpdateMode(false);
    }
    setResponsePage(callerPage);
  }

  public boolean isAlreadySubmitted()
  {
    return alreadySubmitted;
  }

  public void setAlreadySubmitted(final boolean alreadySubmitted)
  {
    this.alreadySubmitted = alreadySubmitted;
  }
}
