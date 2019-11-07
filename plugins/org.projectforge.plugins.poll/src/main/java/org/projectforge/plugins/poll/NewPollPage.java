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

import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteMaxLengthTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.MaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

import java.util.List;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class NewPollPage extends PollBasePage
{
  private static final long serialVersionUID = -3852729293168721111L;

  @SpringBean
  private PollDao pollDao;

  private final NewPollFrontendModel model;

  /**
   * @param parameters
   */
  public NewPollPage(final PageParameters parameters)
  {
    super(parameters);
    model = new NewPollFrontendModel(new PollDO());
    model.getPollDo().setOwner(getUser());
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();

    gridBuilder.newSplitPanel(GridSize.COL50);

    final FieldsetPanel fsTitle = gridBuilder.newFieldset(getString("plugins.poll.new.title"));
    final MaxLengthTextField titleField = new MaxLengthTextField(fsTitle.getTextFieldId(),
        new PropertyModel<>(model.getPollDo(), "title"));
    titleField.setRequired(true);
    fsTitle.add(titleField);

    final FieldsetPanel fsLocation = gridBuilder.newFieldset(getString("plugins.poll.new.location"));
    final PFAutoCompleteMaxLengthTextField locationTextField = new PFAutoCompleteMaxLengthTextField(
        fsLocation.getTextFieldId(),
        new PropertyModel<>(model.getPollDo(), "location"))
    {
      private static final long serialVersionUID = 2008897410054999896L;

      @Override
      protected List<String> getChoices(final String input)
      {
        return pollDao.getAutocompletion("location", input);
      }
    };
    fsLocation.add(locationTextField);

    final FieldsetPanel fsDescription = gridBuilder.newFieldset(getString("plugins.poll.new.description"));
    final MaxLengthTextArea descriptionField = new MaxLengthTextArea(fsDescription.getTextAreaId(),
        new PropertyModel<>(
            model.getPollDo(), "description"));
    fsDescription.add(descriptionField);

  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#isBackButtonVisible()
   */
  @Override
  protected boolean isBackButtonVisible()
  {
    return false;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString("plugins.poll.title");
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onConfirm()
   */
  @Override
  protected void onConfirm()
  {
    if (model.getPollDo().getTitle() == null) {
      this.feedBackPanel.error(getString("plugins.poll.new.error"));
    } else {
      //setResponsePage(new PollEventEditPage(getPageParameters(), model));
    }
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onCancel()
   */
  @Override
  protected void onCancel()
  {
    setResponsePage(PollListPage.class);
  }

  public static void redirectToNewPollPage(final PageParameters parameters)
  {
    throw new RedirectToUrlException(RequestCycle.get().urlFor(NewPollPage.class, parameters).toString());
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onBack()
   */
  @Override
  protected void onBack()
  {
    // do nothing here
  }
}
