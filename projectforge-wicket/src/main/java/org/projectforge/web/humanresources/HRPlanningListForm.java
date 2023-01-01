/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web.humanresources;

import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.ProjektDao;
import org.projectforge.business.humanresources.HRPlanningEntryDO;
import org.projectforge.business.humanresources.HRPlanningEntryDao;
import org.projectforge.business.humanresources.HRPlanningFilter;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.CSSColor;
import org.projectforge.web.calendar.QuickSelectPanel;
import org.projectforge.web.fibu.NewProjektSelectPanel;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.DatePanelSettings;
import org.projectforge.web.wicket.components.LocalDateModel;
import org.projectforge.web.wicket.components.LocalDatePanel;
import org.projectforge.web.wicket.flowlayout.*;
import org.slf4j.Logger;

import java.math.BigDecimal;

/**
 * @author Mario Groß (m.gross@micromata.de)
 */
public class HRPlanningListForm extends AbstractListForm<HRPlanningListFilter, HRPlanningListPage> {
  private static final long serialVersionUID = 3167681159669386691L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HRPlanningListForm.class);

  @SpringBean
  private ProjektDao projektDao;

  @SpringBean
  private HRPlanningEntryDao hrPlanningEntryDao;

  protected LocalDatePanel startDate;

  protected LocalDatePanel stopDate;

  protected NewProjektSelectPanel projektSelectPanel;

  @SuppressWarnings({"serial"})
  @Override
  protected void init() {
    super.init();
    final HRPlanningFilter filter = getSearchFilter();
    if (hrPlanningEntryDao.hasLoggedInUserSelectAccess(false) == false) {
      filter.setUserId(getUser().getId());
    }
    {
      gridBuilder.newSplitPanel(GridSize.COL66);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timePeriod"));
      startDate = new LocalDatePanel(fs.newChildId(), new LocalDateModel(new PropertyModel<>(filter, "startDay")), DatePanelSettings.get()
              .withSelectPeriodMode(true).withRequired(true), true);
      fs.add(startDate);
      fs.setLabelFor(startDate);
      fs.add(new DivTextPanel(fs.newChildId(), " - ").setRenderBodyOnly(false));
      stopDate = new LocalDatePanel(fs.newChildId(), new LocalDateModel(new PropertyModel<>(filter, "stopDay")), DatePanelSettings.get()
              .withSelectPeriodMode(true).withRequired(true), true);
      fs.add(stopDate);
      {
        fs.add(new IconLinkPanel(fs.newChildId(), IconType.REMOVE_SIGN, new ResourceModel("calendar.tooltip.unselectPeriod"), new SubmitLink(
                IconLinkPanel.LINK_ID) {
          @Override
          public void onSubmit() {
            getSearchFilter().setStartDay(null);
            getSearchFilter().setStopDay(null);
            clearInput();
            parentPage.refresh();
          }
        }).setColor(CSSColor.RED));
      }
      final QuickSelectPanel quickSelectPanel = new QuickSelectPanel(fs.newChildId(), parentPage, "quickSelect", startDate);
      fs.add(quickSelectPanel);
      quickSelectPanel.init();
      fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          return WicketUtils.getCalendarWeeks(HRPlanningListForm.this, filter.getStartDay(), filter.getStopDay());
        }
      }));
      fs.add(new HtmlCommentPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          return WicketUtils.getUTCDates(filter.getStartDay(), filter.getStopDay());
        }
      }));
    }
    {
      // Total hours
      gridBuilder.newSplitPanel(GridSize.COL33);
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("timesheet.totalDuration")).suppressLabelForWarning();
      fs.add(new TextPanel(fs.newChildId(), new Model<String>() {
        @Override
        public String getObject() {
          BigDecimal duration = new BigDecimal(0);
          if (parentPage.getList() != null) {
            for (final HRPlanningEntryDO sheet : parentPage.getList()) {
              final BigDecimal temp = sheet.getTotalHours();
              duration = duration.add(temp);
            }
          }
          return duration.toString();
        }
      }));
    }
    boolean showProjectSelectPanel = false;
    final boolean hasFullAccess = parentPage.hasFullAccess();
    if (projektDao.hasLoggedInUserSelectAccess(false) == true) {
      // Project
      showProjectSelectPanel = true;
      if (hasFullAccess == true) {
        gridBuilder.newSplitPanel(GridSize.COL66);
      } else {
        gridBuilder.newGridPanel();
      }
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("fibu.projekt"));
      projektSelectPanel = new NewProjektSelectPanel(fs.newChildId(), new Model<ProjektDO>() {
        @Override
        public ProjektDO getObject() {
          return projektDao.getById(filter.getProjektId());
        }
      }, parentPage, "projektId");
      fs.add(projektSelectPanel);
      projektSelectPanel.init();
    }
    if (hasFullAccess == true) {
      // User
      if (showProjectSelectPanel == true) {
        gridBuilder.newSplitPanel(GridSize.COL33);
      }
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("user"));
      final UserSelectPanel userSelectPanel = new UserSelectPanel(fs.newChildId(), new Model<PFUserDO>() {
        @Override
        public PFUserDO getObject() {
          return UserGroupCache.getInstance().getUser(filter.getUserId());
        }

        @Override
        public void setObject(final PFUserDO object) {
          if (object == null) {
            filter.setUserId(null);
          } else {
            filter.setUserId(object.getId());
          }
        }
      }, parentPage, "userId");
      fs.add(userSelectPanel);
      userSelectPanel.setDefaultFormProcessing(false);
      userSelectPanel.init().withAutoSubmit(true);
    }
  }

  public HRPlanningListForm(final HRPlanningListPage parentPage) {
    super(parentPage);
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#onOptionsPanelCreate(org.projectforge.web.wicket.flowlayout.FieldsetPanel, org.projectforge.web.wicket.flowlayout.DivPanel)
   */
  @Override
  protected void onOptionsPanelCreate(final FieldsetPanel optionsFieldsetPanel, final DivPanel optionsCheckBoxesPanel) {
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
            "groupEntries"), getString("hr.planning.filter.groupEntries")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
            "onlyMyProjects"), getString("hr.planning.filter.onlyMyProjects")));
    optionsCheckBoxesPanel.add(createAutoRefreshCheckBoxButton(optionsCheckBoxesPanel.newChildId(), new PropertyModel<Boolean>(getSearchFilter(),
            "longFormat"), getString("longFormat")));
  }

  @Override
  protected HRPlanningListFilter newSearchFilterInstance() {
    return new HRPlanningListFilter();
  }

  @Override
  protected Logger getLogger() {
    return log;
  }
}
