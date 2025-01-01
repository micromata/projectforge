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

package org.projectforge.business.teamcal.filter;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.Constants;
import org.projectforge.business.teamcal.admin.TeamCalCache;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;

import java.io.Serializable;
import java.util.*;

/**
 * Persist the settings of one named filter entry.
 *
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * @author K. Reinhard (k.reinhard@micromata.de)
 */
public class TemplateEntry implements Serializable, Comparable<TemplateEntry>, Cloneable {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TemplateEntry.class);

    private static final long serialVersionUID = 409057949195992116L;

    private final Set<TemplateCalendarProperties> calendarProperties = new TreeSet<>();

    private Set<Long> visibleCalendarIds;

    @XStreamAsAttribute
    private String name;

    @XStreamAsAttribute
    private Long defaultCalendarId;

    @XStreamAsAttribute
    private Boolean showBirthdays;

    @XStreamAsAttribute
    private Boolean showStatistics;

    @XStreamAsAttribute
    private Long timesheetUserId;

    @XStreamAsAttribute
    private String selectedCalendar;

    @XStreamAsAttribute
    private Boolean showBreaks = true;

    @XStreamAsAttribute
    private Boolean showPlanning;

    public Set<TemplateCalendarProperties> getCalendarProperties() {
        return calendarProperties;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     * @return this for chaining.
     */
    public TemplateEntry setName(final String name) {
        this.name = name;
        return this;
    }

    public TemplateCalendarProperties addNewCalendarProperties(final TeamCalCalendarFilter filter, final Long calId) {
        Objects.requireNonNull(calId);
        final TemplateCalendarProperties props = new TemplateCalendarProperties();
        props.setCalId(calId);
        props.setColorCode(filter.getUsedColor(calId));
        this.calendarProperties.add(props);
        this.visibleCalendarIds = null;
        return props;
    }

    public void removeCalendarProperties(final Long calId) {
        Objects.requireNonNull(calId);
        final TemplateCalendarProperties props = getCalendarProperties(calId);
        if (props != null) {
            this.calendarProperties.remove(props);
        }
        this.visibleCalendarIds = null;
    }

    public String getColorCode(final Long calendarId) {
        final TemplateCalendarProperties props = getCalendarProperties(calendarId);
        if (props == null) {
            return null;
        }
        return props.getColorCode();
    }

    public TemplateCalendarProperties getCalendarProperties(final Long calendarId) {
        if (calendarId == null) {
            return null;
        }
        for (final TemplateCalendarProperties props : calendarProperties) {
            if (calendarId.equals(props.getCalId())) {
                return props;
            }
        }
        return null;
    }

    public boolean contains(final Long calendarId) {
        return getCalendarProperties(calendarId) != null;
    }

    public boolean isVisible(final Long calendarId) {
        final TemplateCalendarProperties props = getCalendarProperties(calendarId);
        return props != null && props.isVisible();
    }

    /**
     * @return the visibleCalendarIds
     */
    public Set<Long> getVisibleCalendarIds() {
        if (this.visibleCalendarIds == null) {
            this.visibleCalendarIds = new HashSet<>();
            for (final TemplateCalendarProperties props : this.calendarProperties) {
                if (props.isVisible()) {
                    this.visibleCalendarIds.add(props.getCalId());
                }
            }
        }
        return this.visibleCalendarIds;
    }

    /**
     * @return All contained calendars (visible and not visible ones).
     */
    public List<TeamCalDO> getCalendars() {
        final List<TeamCalDO> result = new ArrayList<>();
        for (final TemplateCalendarProperties props : this.calendarProperties) {
            final TeamCalDO cal = getTeamCalCache().getCalendar(props.getCalId());
            if (cal != null) {
                result.add(cal);
            } else {
                log.error("Oups, calendar with id " + props.getCalId() + " not found in TeamCalCache.");
            }
        }
        return result;
    }

    /**
     * @return All contained calendars (visible and not visible ones).
     */
    public Set<Long> getCalendarIds() {
        final Set<Long> result = new HashSet<>();
        for (final TemplateCalendarProperties props : this.calendarProperties) {
            result.add(props.getCalId());
        }
        return result;
    }

    /**
     * Should be called every time if new entries are added or the visibility of any entry was changed. After
     * recalculation of visible calendars is forced.
     */
    public void setDirty() {
        this.visibleCalendarIds = null;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final TemplateEntry o) {
        if (this == o) {
            return 0;
        }
        if (this.name == null) {
            if (o.name == null) {
                return 0;
            }
            return -1;
        }
        if (o.name == null) {
            return 1;
        }
        return this.name.compareTo(o.name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final TemplateEntry other = (TemplateEntry) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public TemplateEntry clone() {
        // try {
        final TemplateEntry cloned = new TemplateEntry(); // super.clone();
        cloned.name = this.name;
        cloned.defaultCalendarId = this.defaultCalendarId;
        cloned.showBirthdays = this.showBirthdays;
        cloned.showBreaks = this.showBreaks;
        cloned.showPlanning = this.showPlanning;
        cloned.showStatistics = this.showStatistics;
        cloned.timesheetUserId = this.timesheetUserId;
        for (final TemplateCalendarProperties props : this.calendarProperties) {
            final TemplateCalendarProperties clonedProps = props.clone();
            cloned.calendarProperties.add(clonedProps);
        }
        cloned.setDirty();
        return cloned;
        // } catch (final CloneNotSupportedException ex) {
        // log.error(ex.getMessage(), ex);
        // return null;
        // }
    }

    /**
     * For avoiding reload of Calendar if no changes are detected. (Was für'n Aufwand für so'n kleines Feature...)
     */
    public boolean isModified(final TemplateEntry other) {
        if (!StringUtils.equals(this.name, other.name)) {
            return true;
        }
        if (calendarProperties.size() != other.calendarProperties.size()) {
            return true;
        }
        if (!Objects.equals(defaultCalendarId, other.defaultCalendarId) //
                || !Objects.equals(showBirthdays, other.showBirthdays) //
                || !Objects.equals(showBreaks, other.showBreaks)
                || !Objects.equals(showPlanning, other.showPlanning)
                || !Objects.equals(showStatistics, other.showStatistics)
                || !Objects.equals(timesheetUserId, other.timesheetUserId)) {
            return true;
        }
        final Iterator<TemplateCalendarProperties> it1 = this.calendarProperties.iterator();
        final Iterator<TemplateCalendarProperties> it2 = other.calendarProperties.iterator();
        while (it1.hasNext()) {
            final TemplateCalendarProperties entry1 = it1.next();
            final TemplateCalendarProperties entry2 = it2.next();
            if (entry1.isModified(entry2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the defaultCalendarId
     */
    public Long getDefaultCalendarId() {
        return defaultCalendarId;
    }

    /**
     * @param defaultCalendarId the defaultCalendarId to set
     * @return this for chaining.
     */
    public TemplateEntry setDefaultCalendarId(final Long defaultCalendarId) {
        this.defaultCalendarId = defaultCalendarId;
        return this;
    }

    /**
     * @return the showBirthdays
     */
    public boolean isShowBirthdays() {
        return showBirthdays == Boolean.TRUE;
    }

    /**
     * @param showBirthdays the showBirthdays to set
     * @return this for chaining.
     */
    public TemplateEntry setShowBirthdays(final boolean showBirthdays) {
        this.showBirthdays = showBirthdays;
        return this;
    }

    /**
     * @return the showBreaks
     */
    public boolean isShowBreaks() {
        return showBreaks == Boolean.TRUE;
    }

    /**
     * @param showBreaks the showBreaks to set
     * @return this for chaining.
     */
    public TemplateEntry setShowBreaks(final boolean showBreaks) {
        this.showBreaks = showBreaks;
        return this;
    }

    /**
     * @return the showPlanning
     */
    public boolean isShowPlanning() {
        return showPlanning == Boolean.TRUE;
    }

    /**
     * @param showPlanning the showPlanning to set
     * @return this for chaining.
     */
    public TemplateEntry setShowPlanning(final boolean showPlanning) {
        this.showPlanning = showPlanning;
        return this;
    }

    /**
     * @return the showStatistics
     */
    public boolean isShowStatistics() {
        return showStatistics == Boolean.TRUE;
    }

    /**
     * @param showStatistics the showStatistics to set
     * @return this for chaining.
     */
    public TemplateEntry setShowStatistics(final boolean showStatistics) {
        this.showStatistics = showStatistics;
        return this;
    }

    /**
     * @return the timesheetUserId
     */
    public Long getTimesheetUserId() {
        return timesheetUserId;
    }

    /**
     * Used for users with access to display own and other time-sheets.
     *
     * @param timesheetUserId the timesheetUserId to set
     * @return this for chaining.
     */
    public TemplateEntry setTimesheetUserId(final Long timesheetUserId) {
        this.timesheetUserId = timesheetUserId;
        return this;
    }

    /**
     * @see org.projectforge.business.teamcal.filter.ICalendarFilter#isShowTimesheets()
     */
    public boolean isShowTimesheets() {
        return this.timesheetUserId != null;
    }

    /**
     * @see org.projectforge.business.teamcal.filter.ICalendarFilter#setShowTimesheets(boolean)
     */
    public TemplateEntry setShowTimesheets(final boolean showTimesheets) {
        this.timesheetUserId = ThreadLocalUserContext.getLoggedInUserId();
        return this;
    }

    /**
     * @see org.projectforge.business.teamcal.filter.ICalendarFilter#getSelectedCalendar()
     */
    public String getSelectedCalendar() {
        return selectedCalendar;
    }

    /**
     * @see org.projectforge.business.teamcal.filter.ICalendarFilter#setSelectedCalendar(java.lang.String)
     */
    public TemplateEntry setSelectedCalendar(final String selectedCalendar) {
        this.selectedCalendar = selectedCalendar;
        return this;
    }

    public static String calcCalendarStringForCalendar(final Long calendarId) {
        if (calendarId == null || Constants.isTimesheetCalendarId(calendarId)) {
            return Constants.EVENT_CLASS_NAME;
        } else {
            return String.valueOf(calendarId);
        }
    }

    private TeamCalCache getTeamCalCache() {
        return ApplicationContextProvider.getApplicationContext().getBean(TeamCalCache.class);
    }
}
