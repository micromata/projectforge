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

package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class AjaxIconButtonPanel extends IconButtonPanel
{
  private static final long serialVersionUID = 1366456680979183965L;

  /**
   * @param id
   * @param button
   * @param type
   * @param tooltip
   */
  public AjaxIconButtonPanel(final String id, final Button button, final IconType type, final String tooltip)
  {
    super(id, button, type, tooltip);
  }

  /**
   * @param id
   * @param type
   * @param tooltip
   */
  public AjaxIconButtonPanel(final String id, final IconType type, final IModel<String> tooltip)
  {
    super(id, type, tooltip);
  }

  /**
   * @param id
   * @param type
   * @param tooltip
   */
  public AjaxIconButtonPanel(final String id, final IconType type, final String tooltip)
  {
    super(id, type, tooltip);
  }

  /**
   * @param id
   * @param type
   */
  public AjaxIconButtonPanel(final String id, final IconType type)
  {
    super(id, type);
  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.IconButtonPanel#onSubmit()
   */
  @Override
  protected final void onSubmit()
  {
    // does nothing
  }

  protected void onSubmit(final AjaxRequestTarget target)
  {

  }

  protected void onError(final AjaxRequestTarget target)
  {

  }

  /**
   * @see org.projectforge.web.wicket.flowlayout.IconButtonPanel#createButton(java.lang.String)
   */
  @Override
  protected Button createButton(final String string)
  {
    return new AjaxButton(string) {
      private static final long serialVersionUID = -6046879772559434161L;

      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form< ? > form)
      {
        AjaxIconButtonPanel.this.onSubmit(target);
      }

      @Override
      protected void onError(final AjaxRequestTarget target, final Form< ? > form)
      {
        AjaxIconButtonPanel.this.onError(target);
      }

      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return isButtonVisible();
      }
    };
  }

}
