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

package org.projectforge.plugins.poll;

import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.web.session.MySession;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.projectforge.web.wicket.bootstrap.GridBuilder;
import org.projectforge.web.wicket.components.SingleButtonPanel;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public abstract class PollBasePage extends AbstractSecuredPage
{
  private static final long serialVersionUID = -1876054200928030613L;

  protected GridBuilder gridBuilder;

  protected Form<String> form;

  protected FeedbackPanel feedBackPanel;

  /**
   * @param parameters
   */
  public PollBasePage(final PageParameters parameters)
  {
    super(parameters);
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();

    // Cancel button
    final Button cancel = new Button(SingleButtonPanel.WICKET_ID)
    {
      static final long serialVersionUID = -7779593314951993472L;

      @Override
      public final void onSubmit()
      {
        PollBasePage.this.onCancel();
      }

      @Override
      public final void onError()
      {
        PollBasePage.this.onCancel();
      }
    };
    final SingleButtonPanel cancelPanel = new SingleButtonPanel("cancel", cancel, getString("cancel"),
        SingleButtonPanel.CANCEL);

    // Confirm button
    final Button confirm = new Button(SingleButtonPanel.WICKET_ID)
    {
      static final long serialVersionUID = -7779593314951993472L;

      @Override
      public final void onSubmit()
      {
        PollBasePage.this.onConfirm();
      }

      @Override
      public final void onError()
      {
        PollBasePage.this.onConfirm();
      }
    };
    final SingleButtonPanel confirmPanel = new SingleButtonPanel("confirm", confirm,
        getString("plugins.poll.new.continue"),
        SingleButtonPanel.DEFAULT_SUBMIT);

    // back button
    final Button back = new Button(SingleButtonPanel.WICKET_ID)
    {
      static final long serialVersionUID = -7779593314951993472L;

      @Override
      public final void onSubmit()
      {
        PollBasePage.this.onBack();
      }

      @Override
      public final void onError()
      {
        PollBasePage.this.onBack();
      }
    };
    final SingleButtonPanel backPanel = new SingleButtonPanel("back", back, getString("back"),
        SingleButtonPanel.DEFAULT_SUBMIT);
    backPanel.setVisible(isBackButtonVisible());

    // delete button
    final Button delete = new Button(SingleButtonPanel.WICKET_ID)
    {
      static final long serialVersionUID = -7779593314951993472L;

      @Override
      public final void onSubmit()
      {
        PollBasePage.this.onDelete();
      }

      @Override
      public final void onError()
      {
        PollBasePage.this.onDelete();
      }
    };
    final SingleButtonPanel deletePanel = new SingleButtonPanel("delete", delete, getString("plugins.poll.new.delete"),
        SingleButtonPanel.DEFAULT_SUBMIT);
    deletePanel.setVisible(isDeleteButtonVisible());

    form = new Form<>("pollForm");
    body.add(form);
    form.add(cancelPanel);
    form.add(confirmPanel);
    form.add(backPanel);
    form.add(deletePanel);

    gridBuilder = new GridBuilder(form, "flowform");

    final ContainerFeedbackMessageFilter containerFeedbackMessageFilter = new ContainerFeedbackMessageFilter(this);
    final WebMarkupContainer feedbackContainer = new WebMarkupContainer("feedbackContainer")
    {
      private static final long serialVersionUID = -2676548030393266940L;

      /**
       * @see org.apache.wicket.Component#isVisible()
       */
      @Override
      public boolean isVisible()
      {
        return MySession.get().getFeedbackMessages().hasMessage(containerFeedbackMessageFilter);
      }
    };
    feedBackPanel = new FeedbackPanel("feedback", containerFeedbackMessageFilter);
    feedbackContainer.add(feedBackPanel);
    form.add(feedbackContainer);
  }

  /**
   * @return
   */
  protected boolean isDeleteButtonVisible()
  {
    return false;
  }

  /**
   * @return
   */
  protected boolean isBackButtonVisible()
  {
    return true;
  }

  protected void onDelete()
  {
  };

  protected abstract void onBack();

  protected abstract void onConfirm();

  protected abstract void onCancel();

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return null;
  }
}
