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

import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UsersComparator;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.poll.attendee.PollAttendeeDO;
import org.projectforge.plugins.poll.attendee.PollAttendeeDao;
import org.projectforge.web.common.MultiChoiceListHelper;
import org.projectforge.web.user.UsersProvider;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.autocompletion.PFAutoCompleteTextField;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;
import org.wicketstuff.select2.Select2MultiChoice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class PollEditForm extends AbstractEditForm<PollDO, PollEditPage>
{
  private static final long serialVersionUID = 1268981578238971117L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PollEditForm.class);

  @SpringBean
  protected AccessChecker accessChecker;

  @SpringBean
  protected PollAttendeeDao pollAttendeeDao;

  @SpringBean
  UserDao userDao;

  // @SpringBean
  // protected PollEventDao pollEventDao;

  /**
   * @param parentPage
   * @param data
   */
  public PollEditForm(final PollEditPage parentPage, final PollDO data)
  {
    super(parentPage, data);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#init()
   */
  @Override
  protected void init()
  {
    super.init();

    final Collection<PFUserDO> attendeePFUserList = new ArrayList<>();
    final Collection<String> emailList = new ArrayList<>();
    for (final PollAttendeeDO attendee : pollAttendeeDao.getListByPoll(data)) {
      if (attendee.getUser() != null) {
        attendeePFUserList.add(attendee.getUser());
      } else {
        if (attendee.getEmail() != null) {
          emailList.add(attendee.getEmail());
        }
      }
    }

    gridBuilder.newSplitPanel(GridSize.COL50);

    // new title
    final FieldsetPanel fsTitle = gridBuilder.newFieldset(getString("plugins.poll.new.title"));
    final RequiredTextField<String> title = new RequiredTextField<>(fsTitle.getTextFieldId(),
        new PropertyModel<>(this.data, "title"));
    fsTitle.add(title);

    // new location
    final FieldsetPanel fsLocation = gridBuilder.newFieldset(getString("plugins.poll.new.location"));
    final PFAutoCompleteTextField<String> location = new PFAutoCompleteTextField<String>(fsLocation.getTextFieldId(),
        new PropertyModel<>(
            this.data, "location"))
    {
      private static final long serialVersionUID = -2309992819521957913L;

      @Override
      protected List<String> getChoices(final String input)
      {
        return getBaseDao().getAutocompletion("location", input);
      }
    };
    fsLocation.add(location);

    // new description
    final FieldsetPanel fsDesc = gridBuilder.newFieldset(getString("plugins.poll.new.description"));
    final TextArea<String> desc = new TextArea<>(fsDesc.getTextAreaId(),
        new PropertyModel<>(this.data, "description"));
    fsDesc.add(desc);

    // attendee list
    final FieldsetPanel fsAttendee = gridBuilder.newFieldset(getString("plugins.poll.attendee.users"));
    final UsersProvider usersProvider = new UsersProvider(userDao);
    final MultiChoiceListHelper<PFUserDO> attendeeHelper = new MultiChoiceListHelper<PFUserDO>()
        .setComparator(new UsersComparator())
        .setFullList(usersProvider.getSortedUsers());
    attendeeHelper.setAssignedItems(attendeePFUserList);
    final Select2MultiChoice<PFUserDO> attendees = new Select2MultiChoice<>(
        fsAttendee.getSelect2MultiChoiceId(),
        new PropertyModel<>(attendeeHelper, "assignedItems"), usersProvider);
    fsAttendee.add(attendees);

    // new email list
    // TODO email list
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
