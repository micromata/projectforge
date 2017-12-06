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

package org.projectforge.web.teamcal.dialog;

import java.sql.Timestamp;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.projectforge.business.teamcal.event.model.TeamEvent;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.web.dialog.ModalDialog;
import org.projectforge.web.teamcal.event.RecurrencyChangeType;
import org.projectforge.web.teamcal.event.TeamEventEditPage;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.DivPanel;
import org.projectforge.web.wicket.flowlayout.DivTextPanel;

import de.micromata.wicket.ajax.AjaxCallback;

/**
 * Dialog which appears, when a user tries to modify an recurrent event
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class RecurrenceChangeDialog extends ModalDialog
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RecurrenceChangeDialog.class);

  private static final long serialVersionUID = 7266725860088619248L;

  private TeamEvent event;

  private Timestamp newStartDate, newEndDate;

  private SingleButtonPanel allFutureEventsButtonPanel;

  /**
   * @param id
   * @param titleModel
   */
  public RecurrenceChangeDialog(final String id, final IModel<String> titleModel)
  {
    super(id);
    setTitle(titleModel);
  }

  @Override
  public void init()
  {
    setCloseButtonLabel(getString("cancel"));
    final Form<Void> form = new Form<Void>(getFormId());
    init(form);
    final DivPanel content = gridBuilder.getPanel();
    final DivTextPanel textPanel = new DivTextPanel(content.newChildId(), getString("plugins.teamcal.event.recurrence.change.content"));
    content.add(textPanel);

    // add all change callback
    final AjaxCallback allCallback = new AjaxCallback() {
      private static final long serialVersionUID = 7852511931690947544L;

      @Override
      public void callback(final AjaxRequestTarget target)
      {
        onChangeAllEventsSelected(target, event);
      }
    };
    appendNewAjaxActionButton(allCallback, getString("plugins.teamcal.event.recurrence.change.all"));

    // add future only change callback
    final AjaxCallback futureCallback = new AjaxCallback() {
      private static final long serialVersionUID = 7852511931690947544L;

      @Override
      public void callback(final AjaxRequestTarget target)
      {
        onChangeFutureOnlyEventsSelected(target, event);
      }
    };
    allFutureEventsButtonPanel = appendNewAjaxActionButton(futureCallback, getString("plugins.teamcal.event.recurrence.change.future"));

    // add future only change callback
    final AjaxCallback singleCallback = new AjaxCallback() {
      private static final long serialVersionUID = 7852511931690947544L;

      @Override
      public void callback(final AjaxRequestTarget target)
      {
        onChangeSingleEventSelected(target, event);
      }
    };
    appendNewAjaxActionButton(singleCallback, getString("plugins.teamcal.event.recurrence.change.single"));

  }

  /**
   * @see org.projectforge.web.dialog.ModalDialog#open(org.apache.wicket.ajax.AjaxRequestTarget)
   */
  @Override
  public RecurrenceChangeDialog open(final AjaxRequestTarget target)
  {
    log.error("Dear developer, please use open(target, eventDo).");
    throw new UnsupportedOperationException();
  }

  public void open(final AjaxRequestTarget target, final TeamEvent event, final Timestamp newStartDate, final Timestamp newEndDate)
  {
    this.event = event;
    this.newStartDate = newStartDate;
    this.newEndDate = newEndDate;
    if (event instanceof TeamEventDO) {
      // All future events are the same as all events, because the user selected the first event:
      allFutureEventsButtonPanel.getButton().setVisible(false);
    } else {
      allFutureEventsButtonPanel.getButton().setVisible(true);
    }
    addButtonBar(target);
    super.open(target);
  }

  protected void onChangeAllEventsSelected(final AjaxRequestTarget target, final TeamEvent event)
  {
    onChangeEvents(target, event, RecurrencyChangeType.ALL);
  }

  protected void onChangeFutureOnlyEventsSelected(final AjaxRequestTarget target, final TeamEvent event)
  {
    onChangeEvents(target, event, RecurrencyChangeType.ALL_FUTURE);
  }

  protected void onChangeSingleEventSelected(final AjaxRequestTarget target, final TeamEvent event)
  {
    onChangeEvents(target, event, RecurrencyChangeType.ONLY_CURRENT);
  }

  private void onChangeEvents(final AjaxRequestTarget target, final TeamEvent event, final RecurrencyChangeType recurrencyChangeType) {
    final TeamEventEditPage teamEventEditPage = new TeamEventEditPage(new PageParameters(), event, newStartDate, newEndDate, recurrencyChangeType);
    teamEventEditPage.setReturnToPage(getWebPage());
    setResponsePage(teamEventEditPage);
  }

}
