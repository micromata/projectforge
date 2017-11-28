package org.projectforge.business.teamcal.common;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.fibu.ProjektDO;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.task.TaskDO;
import org.projectforge.business.timesheet.TimesheetDO;
import org.projectforge.business.utils.HtmlHelper;

public class CalendarHelper
{
  public static String getTitle(final TimesheetDO timesheet)
  {
    final Kost2DO kost2 = timesheet.getKost2();
    final TaskDO task = timesheet.getTask();
    if (kost2 == null) {
      return (task != null && task.getTitle() != null) ? HtmlHelper.escapeXml(task.getTitle()) : "";
    }
    final StringBuffer buf = new StringBuffer();
    final StringBuffer b2 = new StringBuffer();
    final ProjektDO projekt = kost2.getProjekt();
    if (projekt != null) {
      if (StringUtils.isNotBlank(projekt.getIdentifier()) == true) {
        b2.append(projekt.getIdentifier());
      } else {
        b2.append(projekt.getName());
      }
    } else {
      b2.append(kost2.getDescription());
    }
    buf.append(StringUtils.abbreviate(b2.toString(), 30));
    return buf.toString();
  }
}
