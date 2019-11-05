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

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.poll.attendee.PollAttendeeDO;
import org.projectforge.plugins.poll.attendee.PollAttendeeDao;
import org.projectforge.plugins.poll.attendee.PollAttendeeDisabledChoiceProvider;
import org.projectforge.plugins.poll.attendee.PollAttendeePage;
import org.projectforge.plugins.poll.event.PollEventDO;
import org.projectforge.plugins.poll.event.PollEventDao;
import org.projectforge.plugins.poll.event.PollEventDisabledChoiceProvider;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.UsersProvider;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.wicketstuff.select2.Select2MultiChoice;

import java.util.*;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class NewPollOverviewPage extends PollBasePage
{
  private static final long serialVersionUID = 7667632498760754905L;

  @SpringBean
  private PollDao pollDao;

  @SpringBean
  private PollAttendeeDao pollAttendeeDao;

  @SpringBean
  private PollEventDao pollEventDao;

  @SpringBean
  UserDao userDao;

  private final NewPollFrontendModel model;

  private boolean isModified;

  /**
   *
   */
  public NewPollOverviewPage(final PageParameters parameters)
  {
    super(parameters);
    if (parameters == null) {
      NewPollPage.redirectToNewPollPage(parameters);
      this.model = null;
    } else {
      final Integer id = new Integer(parameters.get("id").toString());
      this.model = new NewPollFrontendModel(pollDao.getById(id));
      this.model.initModelByPoll();
    }
  }

  public NewPollOverviewPage(final PageParameters parameters, final NewPollFrontendModel model)
  {
    super(parameters);
    this.model = model;
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();

    if (!model.isNew()) {
      isModified = isModelModified();
    }

    final FieldsetPanel fsTitle = gridBuilder.newFieldset(getString("plugins.poll.new.title"));
    final TextField<String> title = new TextField<>(fsTitle.getTextFieldId(),
        new PropertyModel<>(model.getPollDo(), "title"));
    title.setEnabled(this.model.isNew());
    fsTitle.add(title);

    final FieldsetPanel fsLocation = gridBuilder.newFieldset(getString("plugins.poll.new.location")).setLabelFor(this);
    final TextField<String> location = new TextField<>(fsLocation.getTextFieldId(),
        new PropertyModel<>(model.getPollDo(), "location"));
    location.setEnabled(this.model.isNew());
    fsLocation.add(location);

    final FieldsetPanel fsDescription = gridBuilder.newFieldset(getString("plugins.poll.new.description"));
    final TextArea<String> description = new TextArea<>(fsDescription.getTextAreaId(),
        new PropertyModel<>(this.model.getPollDo(),
            "description"));
    description.setEnabled(this.model.isNew());
    fsDescription.add(description);

    gridBuilder.newGridPanel();

    //    if (this.model.isNew() == true) {
    final FieldsetPanel fsUsers = gridBuilder.newFieldset(getString("plugins.poll.attendee.users"));

    //    if (model.isNew() == false && isModified == false) {
    createDisabledChoices(fsUsers, model.getPollAttendeeList(), true);
    //    } else {
    //      createDisabledChoices(fsUsers, model.getCalculatedAttendeeList(), true);
    //    }
    //    } else {
    //      createEnabledChoices();
    //    }

    final FieldsetPanel fsEMails = gridBuilder.newFieldset(getString("plugins.poll.attendee.emails"));
    //    if (model.isNew() == false && isModified == false) {
    createDisabledChoices(fsEMails, model.getPollAttendeeList(), false);
    //    } else {
    //      createDisabledChoices(fsEMails, model.getCalculatedAttendeeList(), false);
    //    }

    final FieldsetPanel fsEvents = gridBuilder.newFieldset(getString("plugins.poll.attendee.events"));
    createDisabledChoices(fsEvents, model.getAllEvents());
  }

  /**
   * @param fieldset
   * @param modelList
   * @param b
   */
  private void createDisabledChoices(final FieldsetPanel fieldset, final List<PollAttendeeDO> rawList,
      final boolean isUser)
  {
    final List<PollAttendeeDO> modelList = new LinkedList<>();
    for (final PollAttendeeDO attendee : rawList) {
      if (attendee.getUser() != null && isUser) {
        modelList.add(attendee);
      } else if (attendee.getEmail() != null && !isUser) {
        modelList.add(attendee);
      }
    }
    final MultiChoiceListHelper<PollAttendeeDO> assignHelper = new MultiChoiceListHelper<PollAttendeeDO>()
        .setAssignedItems(modelList);
    final Select2MultiChoice<PollAttendeeDO> multiChoices = new Select2MultiChoice<>(
        fieldset.getSelect2MultiChoiceId(),
        new PropertyModel<>(assignHelper, "assignedItems"),
        new PollAttendeeDisabledChoiceProvider(modelList));
    fieldset.add(multiChoices);
    multiChoices.setEnabled(false);
  }

  private void createEnabledChoices()
  {
    final UsersProvider usersProvider = new UsersProvider(userDao);
    // User select
    final FieldsetPanel fsUserSelect = gridBuilder.newFieldset(getString("plugins.poll.attendee.users"));
    final MultiChoiceListHelper<PFUserDO> assignUsersListHelper = new MultiChoiceListHelper<PFUserDO>()
        .setComparator(new UsersComparator())
        .setFullList(usersProvider.getSortedUsers());
    final HashSet<PFUserDO> attendeess = new HashSet<>();
    for (final PollAttendeeDO attendee : model.getPollAttendeeList()) {
      if (attendee.getUser() != null) {
        attendeess.add(attendee.getUser());
      } else {
        // TODO email list
      }
    }
    assignUsersListHelper.setAssignedItems(attendeess);
    final Select2MultiChoice<PFUserDO> users = new Select2MultiChoice<>(fsUserSelect.getSelect2MultiChoiceId(),
        new PropertyModel<>(assignUsersListHelper, "assignedItems"), usersProvider);
    fsUserSelect.add(users);
  }

  /**
   * @param fieldset
   * @param modelList
   * @param b
   */
  private void createDisabledChoices(final FieldsetPanel fieldset, final List<PollEventDO> modelList)
  {
    final MultiChoiceListHelper<PollEventDO> assignHelper = new MultiChoiceListHelper<PollEventDO>()
        .setAssignedItems(modelList);
    final Select2MultiChoice<PollEventDO> multiChoices = new Select2MultiChoice<>(
        fieldset.getSelect2MultiChoiceId(),
        new PropertyModel<>(assignHelper, "assignedItems"),
        new PollEventDisabledChoiceProvider(modelList));
    fieldset.add(multiChoices);
    multiChoices.setEnabled(false);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractUnsecureBasePage#getTitle()
   */
  @Override
  protected String getTitle()
  {
    return getString("plugins.poll.result");
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onConfirm()
   */
  @Override
  protected void onConfirm()
  {
    final boolean isNew = model.isNew();

    pollDao.saveOrUpdate(model.getPollDo());

    // relate elements with poll
    if (isNew || isModified) {
      for (final PollEventDO event : model.getAllEvents()) {
        if (event.getPoll() == null) {
          event.setPoll(model.getPollDo());
        }
      }
      for (final PollAttendeeDO attendee : model.getPollAttendeeList()) {
        if (attendee.getPoll() == null) {
          attendee.setPoll(model.getPollDo());
        }
      }
      pollEventDao.saveOrUpdate(model.getAllEvents());
      pollAttendeeDao.saveOrUpdate(model.getPollAttendeeList());
    }

    setResponsePage(PollListPage.class);
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onCancel()
   */
  @Override
  protected void onCancel()
  {
    setResponsePage(PollListPage.class);
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onBack()
   */
  @Override
  protected void onBack()
  {
    setResponsePage(new PollAttendeePage(getPageParameters(), model));
  }

  /**
   * @see org.projectforge.plugins.poll.PollBasePage#onDelete()
   */
  @Override
  protected void onDelete()
  {
    if (model != null && model.getPollDo() != null) {
      model.getPollDo().setDeleted(true);
      pollDao.save(model.getPollDo());
    }
  }

  /**
   * Validate if something was changed.
   *
   * @return
   */
  private boolean isModelModified()
  {
    if (model.getPollDo().getId() != null) {
      final PollDO poll = model.getPollDo();
      final PollDO pollOld = pollDao.getById(model.getPollDo().getId());
      final List<PollAttendeeDO> attendees = pollAttendeeDao.getListByPoll(poll);
      final List<PollEventDO> events = pollEventDao.getListByPoll(poll);

      // compare attendees
      final boolean compareAttendees = compareLists(attendees, model.getPollAttendeeList());

      // compare events
      final boolean compareEvents = compareLists(events, model.getAllEvents());

      // compare poll
      final boolean comparePoll = Objects.equals(pollOld, poll);

      if (!compareAttendees || !compareEvents || !comparePoll) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * compare lists and their elements. returns true, if lists are identical, false else.
   *
   * @param listA
   * @param listB
   * @return
   */
  private boolean compareLists(final List<?> listA, final List<?> listB)
  {
    if (listA.size() == listB.size()) {
      for (final Object obj : listB) {
        if (!listA.contains(obj)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }
}
