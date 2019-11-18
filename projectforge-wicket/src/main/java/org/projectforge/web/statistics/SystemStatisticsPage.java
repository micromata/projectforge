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

package org.projectforge.web.statistics;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.tasktree.TaskTreeHelper;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.framework.persistence.api.HibernateUtils;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.time.DateHelper;
import org.projectforge.framework.utils.NumberFormatter;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.wicket.AbstractSecuredPage;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;

public class SystemStatisticsPage extends AbstractSecuredPage {
  private static final long serialVersionUID = 8587252641914110851L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SystemStatisticsPage.class);

  @SpringBean
  private DataSource dataSource;

  public SystemStatisticsPage(final PageParameters parameters) {
    super(parameters);
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    body.add(new Label("totalNumberOfTimesheets", NumberFormatter.format(getTableCount(jdbc, TimesheetDO.class))));
    final TaskTree taskTree = TaskTreeHelper.getTaskTree();
    final long totalDuration = taskTree.getRootTaskNode().getDuration(taskTree, true);
    BigDecimal tatalPersonDays = new BigDecimal(totalDuration).divide(DateHelper.SECONDS_PER_WORKING_DAY, 2,
            BigDecimal.ROUND_HALF_UP);
    tatalPersonDays = NumberHelper.setDefaultScale(tatalPersonDays);
    body.add(new Label("totalNumberOfTimesheetDurations",
            NumberHelper.getNumberFractionFormat(getLocale(), tatalPersonDays.scale())
                    .format(tatalPersonDays)));
    body.add(new Label("totalNumberOfUsers", NumberFormatter.format(getTableCount(jdbc, PFUserDO.class))));
    body.add(new Label("totalNumberOfTasks", NumberFormatter.format(getTableCount(jdbc, TaskDO.class))));
    final int totalNumberOfHistoryEntries = getTableCount(jdbc, PfHistoryMasterDO.class)
            + getTableCount(jdbc, PfHistoryMasterDO.class);
    body.add(new Label("totalNumberOfHistoryEntries", NumberFormatter.format(totalNumberOfHistoryEntries)));

    RepeatingView listItems = new RepeatingView("memoryStatisticsIterator");
    body.add(listItems);
    System.gc();
    for (MemoryPoolMXBean mpBean : ManagementFactory.getMemoryPoolMXBeans()) {
      if (mpBean.getType() == MemoryType.HEAP) {
        MemoryUsage usageBean = mpBean.getUsage();
        String usage = new StringBuilder()
                .append("max=").append(NumberHelper.formatBytes(usageBean.getMax()))
                .append(", used=").append(NumberHelper.formatBytes(usageBean.getUsed()))
                .append(", committed=").append(NumberHelper.formatBytes(usageBean.getCommitted()))
                .append(", init=").append(NumberHelper.formatBytes(usageBean.getInit()))
                .toString();
        WebMarkupContainer row = new WebMarkupContainer(listItems.newChildId());
        listItems.add(row);
        row.add(new Label("memoryType", "Memory " + mpBean.getName()));
        row.add(new Label("memoryStatistics", usage));
        log.info("Memory: " + usage);
      }
    }
  }

  private int getTableCount(final JdbcTemplate jdbc, final Class<?> entity) {
    try {
      return jdbc.queryForObject("SELECT COUNT(*) FROM " + HibernateUtils.getDBTableName(entity), Integer.class);
    } catch (final Exception ex) {
      log.error(ex.getMessage(), ex);
      return 0;
    }
  }

  @Override
  protected String getTitle() {
    return getString("system.statistics.title");
  }

}
