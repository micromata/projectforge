/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.ihk;

import com.google.gson.Gson;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.time.TimePeriod;
import org.projectforge.web.CSSColor;
import org.projectforge.web.calendar.QuickSelectWeekPanel;
import org.projectforge.web.wicket.AbstractStandardForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.components.LocalDateModel;
import org.projectforge.web.wicket.components.LocalDatePanel;
import org.projectforge.web.wicket.components.SingleButtonPanel;
import org.projectforge.web.wicket.flowlayout.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * Created by mnuhn on 05.12.2019
 * Updated by mweishaar, jhpeters and mopreusser on 28.05.2020
 */
public class IHKForm extends AbstractStandardForm<Object, IHKPage> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IHKForm.class);

    private TimePeriod timePeriod = new TimePeriod();

    protected LocalDatePanel startDate;

    protected LocalDatePanel stopDate;

    protected LocalDate ausbildungsbeginn;

    protected String teamname;

    protected int ausbildungsjahr;

    public IHKForm(IHKPage parentPage) {
        super(parentPage);
    }

    @SpringBean
    private AddressDao addressDao;

    @Override
    protected void init() {
        super.init();

        String userComment = "";
        List<AddressDO> addressDos = addressDao.getList(new BaseSearchFilter());
        boolean foundUser = false;

        for (AddressDO addressDo : addressDos) {
            if (addressDo.getName().equals(ThreadLocalUserContext.getUser().getLastname())) {
                if (addressDo.getFirstName().equals(ThreadLocalUserContext.getUser().getFirstname())) {
                    userComment = addressDo.getComment();
                    foundUser = true;
                    break;
                }
            }
        }

        if (!(userComment == null || userComment.isEmpty())) {
            try {
                IHKCommentObject ihkCommentObject;
                Gson gson = new Gson();
                ihkCommentObject = gson.fromJson(userComment, IHKCommentObject.class);
                ausbildungsjahr = ihkCommentObject.getAusbildungsjahr();
                teamname = ihkCommentObject.getTeamname();
                ausbildungsbeginn = LocalDate.parse(ihkCommentObject.getAusbildungStartDatum());
            } catch (Exception e) {
                log.warn("IHK-Plugin: wasnt able to parse json from AddressDo.getComment():" + e.getMessage());
                throw new UserException("plugins.ihk.jsonError.parsing", e.getMessage());
            }
        } else {
            if (foundUser) {
                log.info("IHK-Plugin: userComment not set. Value was null or empty.");
                throw new UserException("plugins.ihk.jsonError.emtpy");
            } else {
                log.info("IHK-Plugin: userComment not set. Value was null or empty.");
                throw new UserException("plugins.ihk.userError.notFound");
            }


        }

        gridBuilder.newSplitPanel(GridSize.COL66);
        final FieldsetPanel fs = gridBuilder.newFieldset(getString("timePeriod"));

        FieldProperties<LocalDate> props = getFromDayProperties();
        startDate = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
        fs.add(startDate);
        fs.setLabelFor(startDate);
        fs.add(new DivTextPanel(fs.newChildId(), " - "));
        props = getToDayProperties();
        stopDate = new LocalDatePanel(fs.newChildId(), new LocalDateModel(props.getModel()));
        fs.add(stopDate);

        {
            final SubmitLink unselectPeriodLink = new SubmitLink(IconLinkPanel.LINK_ID) {
                @Override
                public void onSubmit() {
                    timePeriod.setFromDate(null);
                    timePeriod.setToDate(null);
                    clearInput();
                }
            };
            unselectPeriodLink.setDefaultFormProcessing(false);
            fs.add(new IconLinkPanel(fs.newChildId(), IconType.REMOVE_SIGN,
                    new ResourceModel("calendar.tooltip.unselectPeriod"),
                    unselectPeriodLink).setColor(CSSColor.RED));
        }

        final QuickSelectWeekPanel quickSelectWeekPanel = new QuickSelectWeekPanel(fs.newChildId(), new Model<LocalDate>() {
            @Override
            public LocalDate getObject() {
                startDate.getDateField().validate();
                return PFDay.fromOrNow(startDate.getDateField().getConvertedInput()).getLocalDate();
            }
        }, parentPage, "quickSelect" + ".week");
        fs.add(quickSelectWeekPanel);
        quickSelectWeekPanel.init();

        fs.add(new DivTextPanel(fs.newChildId(), new Model<String>() {
            @Override
            public String getObject() {
                return WicketUtils.getCalendarWeeks(IHKForm.this, timePeriod.getFromDate(), timePeriod.getToDate());
            }
        }));

        Button downloadButton = new Button(SingleButtonPanel.WICKET_ID, new Model("download")) {
            @Override
            public void onSubmit() {
                parentPage.export(false);
            }
        };
        fs.add(new SingleButtonPanel(fs.newChildId(), downloadButton,
                getString("plugins.ihk.download"), SingleButtonPanel.DEFAULT_SUBMIT));
    }

    void showMissingDescriptionList(List<TimesheetDO> missingDescriptionList) {
        gridBuilder.newSplitPanel(GridSize.COL66);

        DateFormat date = new SimpleDateFormat("dd.MM.yyyy");
        DateFormat time = new SimpleDateFormat("HH:mm");

        for (TimesheetDO ts : missingDescriptionList) {

            String label = getString("plugins.ihk.nodescriptionfound");
            if (ts.getKost2() != null) {
                if (ts.getKost2().getDescription() != null) {
                    label = ts.getKost2().getDescription();
                } else if (ts.getKost2().getDisplayName() != null) {
                    label = ts.getKost2().getDisplayName();
                }
            }
            final FieldsetPanel fs = gridBuilder.newFieldset(label);
            fs.add(new DivTextPanel(fs.newChildId(),
                    date.format(ts.getStartTime().getTime()) + " " +
                            time.format(ts.getStartTime().getTime()) + " - " +
                            time.format(ts.getStopTime().getTime())));

            Button editButton = new Button(SingleButtonPanel.WICKET_ID, new Model("edit")) {
                @Override
                public void onSubmit() {
                    parentPage.edit(ts);
                }
            };
            fs.add(new SingleButtonPanel(fs.newChildId(), editButton,
                    getString("plugins.ihk.edit"), SingleButtonPanel.CANCEL));
        }

        // Download anyway button
        final FieldsetPanel downloadPanel = gridBuilder.newFieldset("");
        Button downloadButton = new Button(SingleButtonPanel.WICKET_ID, new Model("download")) {
            @Override
            public void onSubmit() {
                parentPage.export(true);
            }
        };
        downloadPanel.add(new SingleButtonPanel(downloadPanel.newChildId(), downloadButton,
                getString("plugins.ihk.downloadAnyway"), SingleButtonPanel.DEFAULT_SUBMIT));


    }

    private FieldProperties<LocalDate> getToDayProperties() {
        return new FieldProperties<>("", new PropertyModel<>(timePeriod, "fromDay"));
    }

    private FieldProperties<LocalDate> getFromDayProperties() {
        return new FieldProperties<>("", new PropertyModel<>(timePeriod, "toDay"));
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public LocalDatePanel getStartDate() {
        return startDate;
    }

    public LocalDate getAusbildungsbeginn() {
        return ausbildungsbeginn;
    }

    public int getAusbildungsjahr() {
        return ausbildungsjahr;
    }

    public String getTeamname() {
        return teamname;
    }

}
