/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.gantt;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.time.PFDay;
import org.projectforge.framework.time.PFDayUtils;
import org.projectforge.framework.utils.NumberHelper;

import java.io.Serializable;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class GanttUtils {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GanttUtils.class);

  public static Comparator<GanttTask> GANTT_OBJECT_COMPARATOR = new Comparator<GanttTask>() {
    @Override
    public int compare(final GanttTask o1, final GanttTask o2) {
      if (Objects.equals(o1.getId(), o2.getId())) {
        return 0;
      }
      final LocalDate start1 = o1.getCalculatedStartDate();
      final LocalDate start2 = o2.getCalculatedStartDate();
      if (start1 == null) {
        if (start2 != null) {
          return 1;
        }
      } else if (start2 == null) {
        return -1;
      } else {
        final int result = start1.compareTo(start2);
        if (result != 0) {
          return result;
        }
      }
      final LocalDate end1 = o1.getCalculatedEndDate();
      final LocalDate end2 = o2.getCalculatedEndDate();
      if (end1 == null) {
        if (end2 != null) {
          return 1;
        }
      } else if (end2 == null) {
        return -1;
      } else {
        final int result = end1.compareTo(end2);
        if (result != 0) {
          return result;
        }
      }
      if (!StringUtils.equals(o1.getTitle(), o2.getTitle())) {
        return StringHelper.compareTo(o1.getTitle(), o2.getTitle());
      }
      return StringHelper.compareTo(String.valueOf(o1.getId()), String.valueOf(o2.getId()));
    }
  };

  /**
   * Please note: If the start date is after the start date of the earliest task of any child then the start date of this child is returned.
   * Otherwise the set start date or if not set the calculated start date is returned.
   *
   * @param node
   */
  public static LocalDate getCalculatedStartDate(final GanttTask node) {
    final LocalDate start = getCalculatedStartDate(node, new HashSet<>(), new HashSet<>());
    return start;
  }

  private static LocalDate getCalculatedStartDate(final GanttTask node, final Set<Serializable> startDateSet, final Set<Serializable> endDateSet) {
    if (node == null) {
      return null;
    }
    if (node.getStartDate() != null) {
      return node.getStartDate();
    }
    if (node.isStartDateCalculated()) {
      return node.getCalculatedStartDate();
    }
    final int durationDays = node.getDuration() != null ? node.getDuration().setScale(0, RoundingMode.HALF_UP).intValue() : 0;
    if (node.getDuration() != null && node.getEndDate() != null) {
      final LocalDate startDate = calculateDate(node.getEndDate(), -durationDays);
      node.setCalculatedStartDate(startDate).setStartDateCalculated(true);
      if (log.isDebugEnabled()) {
        log.debug("calculated start date=" + startDate + " for: " + node);
      }
      return startDate;
    }
    if (startDateSet.contains(node.getId())) {
      log.error("Circular reference detection (couldn't calculate start date: " + node);
      return null;
    } else {
      startDateSet.add(node.getId());
    }
    LocalDate startDate = null;
    final GanttTask predecessor = node.getPredecessor();
    if (predecessor != null) {
      startDate = getPredecessorRelDate(node.getRelationType(), predecessor, startDateSet, endDateSet);
      if (startDate != null) {
        if (NumberHelper.isNotZero(node.getPredecessorOffset())) {
          startDate = calculateDate(startDate, node.getPredecessorOffset());
        }
        if (node.getRelationType() == GanttRelationType.START_FINISH || node.getRelationType() == GanttRelationType.FINISH_FINISH) {
          if (durationDays > 0) {
            startDate = calculateDate(startDate, -durationDays);
          }
        }
      }
    }
    if ((predecessor == null || (node.getRelationType() != null && node.getRelationType().isIn(GanttRelationType.FINISH_FINISH,
            GanttRelationType.START_FINISH)))
            && node.getChildren() != null) {
      // Calculate start date from the earliest child.
      for (final GanttTask child : node.getChildren()) {
        final LocalDate date = getCalculatedStartDate(child, startDateSet, endDateSet);
        if (startDate == null) {
          startDate = date;
        } else if (date != null && date.isBefore(startDate)) {
          if (log.isDebugEnabled()) {
            log.debug("Start date of child is before start date=" + date + " of parent: " + child);
          }
          startDate = date;
        }
      }
    }
    if (startDate == null && node.getDuration() != null) {
      final LocalDate calculatedEndDate = getCalculatedEndDate(node, startDateSet, endDateSet);
      if (calculatedEndDate != null) {
        startDate = calculateDate(calculatedEndDate, -durationDays);
      }
    }
    node.setCalculatedStartDate(startDate).setStartDateCalculated(true);
    if (log.isDebugEnabled()) {
      log.debug("calculated start date=" + startDate + " for: " + node);
    }
    return startDate;
  }

  private static LocalDate getPredecessorRelDate(final GanttRelationType relationType, final GanttTask predecessor,
                                            final Set<Serializable> startDateSet, final Set<Serializable> endDateSet) {
    if (relationType == GanttRelationType.START_START || relationType == GanttRelationType.START_FINISH) {
      final LocalDate calculatedStartDate = getCalculatedStartDate(predecessor, startDateSet, endDateSet);
      return calculatedStartDate;
    } else {
      final LocalDate calculatedEndDate = getCalculatedEndDate(predecessor, startDateSet, endDateSet);
      return calculatedEndDate;
    }
  }

  /**
   * Calculates the end date. If the end date is set then this value is returned (ignoring the duration in days). If the end date is not
   * given, then the start date is taken and durationDays (only working days) will be added. If no start date is given, the start date will
   * be calculated from the node this node depends on.
   */
  public static LocalDate getCalculatedEndDate(final GanttTask node) {
    final LocalDate end = getCalculatedEndDate(node, new HashSet<>(), new HashSet<>());
    return end;
  }

  /**
   * @param node
   */
  private static LocalDate getCalculatedEndDate(final GanttTask node, final Set<Serializable> startDateSet, final Set<Serializable> endDateSet) {
    if (node == null) {
      return null;
    }
    if (node.getEndDate() != null) {
      return node.getEndDate();
    }
    if (node.isEndDateCalculated()) {
      return node.getCalculatedEndDate();
    }
    final int durationDays = node.getDuration() != null ? node.getDuration().setScale(0, RoundingMode.HALF_UP).intValue() : 0;
    if (node.getDuration() != null && node.getStartDate() != null) {
      final LocalDate endDate = calculateDate(node.getStartDate(), durationDays);
      node.setCalculatedEndDate(endDate).setEndDateCalculated(true);
      if (log.isDebugEnabled()) {
        log.debug("calculated end date=" + endDate + " for: " + node);
      }
      return endDate;
    }
    if (endDateSet.contains(node.getId())) {
      log.error("Circular reference detection (couldn't calculate end date: " + node);
      return null;
    } else {
      endDateSet.add(node.getId());
    }
    LocalDate endDate = null;
    final GanttTask predecessor = node.getPredecessor();
    if (predecessor != null) {
      endDate = getPredecessorRelDate(node.getRelationType(), predecessor, startDateSet, endDateSet);
      if (endDate != null) {
        if (NumberHelper.isNotZero(node.getPredecessorOffset())) {
          endDate = calculateDate(endDate, node.getPredecessorOffset());
        }
        if (node.getRelationType() != GanttRelationType.START_FINISH && node.getRelationType() != GanttRelationType.FINISH_FINISH) {
          if (durationDays > 0) {
            endDate = calculateDate(endDate, durationDays);
          }
        }
      }
    }
    if ((predecessor == null || (node.getRelationType() == null || !node.getRelationType().isIn(GanttRelationType.FINISH_FINISH,
            GanttRelationType.START_FINISH)))
            && node.getChildren() != null
            && node.getDuration() == null) {
      // There are children and the end date is not fix defined by a predecessor.
      for (final GanttTask child : node.getChildren()) {
        final LocalDate date = getCalculatedEndDate(child, startDateSet, endDateSet);
        if (date != null && (endDate == null || date.isAfter(endDate))) {
          endDate = date;
        }
      }
    }
    if (endDate == null && node.getDuration() != null) {
      final LocalDate calculatedStartDate = getCalculatedStartDate(node, startDateSet, endDateSet);
      if (calculatedStartDate != null) {
        endDate = calculateDate(calculatedStartDate, durationDays);
      }
    }
    node.setCalculatedEndDate(endDate).setEndDateCalculated(true);
    if (log.isDebugEnabled()) {
      log.debug("calculated end date=" + endDate + " for: " + node);
    }
    return endDate;
  }

  private static LocalDate calculateDate(final LocalDate date, final int workingDayOffset) {
    PFDay day = PFDay.from(date); // not null
    day = PFDayUtils.addWorkingDays(day, workingDayOffset);
    return day.getDate();
  }
}
