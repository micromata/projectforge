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
import org.apache.wicket.markup.html.form.IFormSubmitter;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.web.wicket.CsrfTokenHandler;
import org.projectforge.web.wicket.mobileflowlayout.MobileGridBuilder;

public abstract class AbstractMobileEditForm<O extends AbstractBaseDO<?>, P extends AbstractMobileEditPage<?, ?, ?>> extends
    AbstractMobileForm<O, P>
{
  private static final long serialVersionUID = 1836099012618517190L;

  protected O data;

  protected MobileGridBuilder gridBuilder;

  /**
   * Cross site request forgery token.
   */
  private final CsrfTokenHandler csrfTokenHandler;

  public AbstractMobileEditForm(final P parentPage, final O data)
  {
    super(parentPage);
    this.data = data;
    csrfTokenHandler = new CsrfTokenHandler(this);
  }

  /**
   * Check the CSRF token right before the onSubmit methods are called, otherwise it may be too late.
   */
  @Override
  protected void delegateSubmit(IFormSubmitter submittingComponent)
  {
    csrfTokenHandler.onSubmit();
    super.delegateSubmit(submittingComponent);
  }

  public O getData()
  {
    return this.data;
  }

  public void setData(final O data)
  {
    this.data = data;
  }

  public boolean isNew()
  {
    return this.data == null || this.data.getId() == null;
  }

  @SuppressWarnings("serial")
  protected void init()
  {
    add(new FeedbackPanel("feedback").setOutputMarkupId(true));
    final SubmitLink submitButton = new SubmitLink("submitButton")
    {
      @Override
      public final void onSubmit()
      {
        parentPage.save();
      }
    };
    final RepeatingView flowform = new RepeatingView("flowform");
    add(flowform);
    gridBuilder = newGridBuilder(flowform);

    add(submitButton);
    if (isNew() == true) {
      submitButton.add(new Label("label", getString("create")));
    } else {
      submitButton.add(new Label("label", getString("update")));
    }
  }
}
