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

package org.projectforge.framework.persistence.api;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Base search filter supported by the DAO's for filtering the result lists. The search filter will be translated via
 * QueryFilter into hibernate query criterias.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class BaseSearchFilter implements Serializable {
  private static final long serialVersionUID = 5970378227395811426L;

  protected String searchString;

  protected boolean deleted = false; // Initialization unnecessary but for documentation.

  protected boolean ignoreDeleted = false; // Initialization unnecessary but for documentation.

  protected int maxRows = -1;

  protected transient boolean sortAndLimitMaxRowsWhileSelect = false;

  protected boolean useModificationFilter;

  protected Integer modifiedByUserId;

  @Deprecated
  protected Date startTimeOfLastModification;

  @Deprecated
  protected Date stopTimeOfLastModification;

  protected Date startTimeOfModification;

  protected Date stopTimeOfModification;

  protected Date modifiedSince;

  private boolean searchHistory;

  private String errorMessage;

  private List<SortProperty> sortProperties;

  private transient String[] searchFields;

  public BaseSearchFilter() {
  }

  public BaseSearchFilter(final BaseSearchFilter filter) {
    if (filter == null) {
      return;
    }
    copyBaseSearchFieldsFrom(filter);
  }

  public void copyBaseSearchFieldsFrom(final BaseSearchFilter filter) {
    this.searchString = filter.searchString;
    this.deleted = filter.deleted;
    this.ignoreDeleted = filter.ignoreDeleted;
    this.maxRows = filter.maxRows;
    this.sortProperties = filter.sortProperties;
    this.sortAndLimitMaxRowsWhileSelect = filter.sortAndLimitMaxRowsWhileSelect;
    this.useModificationFilter = filter.useModificationFilter;
    this.modifiedByUserId = filter.modifiedByUserId;
    this.startTimeOfModification = filter.startTimeOfModification;
    this.stopTimeOfModification = filter.stopTimeOfModification;
    this.searchHistory = filter.searchHistory;
  }

  /**
   * @return this for chaining.
   */
  public BaseSearchFilter reset() {
    deleted = false;
    ignoreDeleted = false;
    searchString = "";
    searchHistory = false;
    return this;
  }

  public boolean isSearchNotEmpty() {
    return StringUtils.isNotEmpty(searchString);
  }

  public String getSearchString() {
    return searchString;
  }

  /**
   * @param searchString
   * @return this for chaining.
   */
  public BaseSearchFilter setSearchString(final String searchString) {
    this.searchString = searchString;
    return this;
  }

  /**
   * @param searchFields
   * @return this for chaining.
   */
  public BaseSearchFilter setSearchFields(final String... searchFields) {
    this.searchFields = searchFields;
    return this;
  }

  /**
   * If not null and a query string for a full text index search is given, then only the given search fields are used
   * instead of the default search fields of the dao.
   *
   * @return
   */
  public String[] getSearchFields() {
    return searchFields;
  }

  /**
   * If given {@link BaseDao#getList(BaseSearchFilter)} will only search for entries which last date of modification
   * AbstractBaseDO.getLastUpdate() isn't before given date.
   *
   * @return the modifiedSince
   */
  public Date getModifiedSince() {
    return modifiedSince;
  }

  /**
   * @param modifiedSince the modifiedSince to set
   * @return this for chaining.
   */
  public BaseSearchFilter setModifiedSince(final Date modifiedSince) {
    this.modifiedSince = modifiedSince;
    return this;
  }

  /**
   * If true then modifiedByUser and time of last modification is used for filtering.
   *
   * @return
   */
  public boolean isUseModificationFilter() {
    return useModificationFilter;
  }

  public boolean applyModificationFilter() {
    return this.useModificationFilter && (this.startTimeOfModification != null || this.stopTimeOfModification != null || this.modifiedByUserId != null);
  }

  /**
   * @param useModificationFilter
   * @return this for chaining.
   */
  public BaseSearchFilter setUseModificationFilter(final boolean useModificationFilter) {
    this.useModificationFilter = useModificationFilter;
    return this;
  }

  public Integer getModifiedByUserId() {
    return modifiedByUserId;
  }

  /**
   * @param modifiedByUserId
   * @return this for chaining.
   */
  public BaseSearchFilter setModifiedByUserId(final Integer modifiedByUserId) {
    this.modifiedByUserId = modifiedByUserId;
    return this;
  }

  public Date getStartTimeOfModification() {
    return startTimeOfModification;
  }

  /**
   * @param startTimeOfModification
   * @return this for chaining.
   */
  public BaseSearchFilter setStartTimeOfModification(final Date startTimeOfModification) {
    this.startTimeOfModification = startTimeOfModification;
    return this;
  }

  public Date getStopTimeOfModification() {
    return stopTimeOfModification;
  }

  /**
   * @param stopTimeOfModification
   * @return this for chaining.
   */
  public BaseSearchFilter setStopTimeOfModification(final Date stopTimeOfModification) {
    this.stopTimeOfModification = stopTimeOfModification;
    return this;
  }

  /**
   * If true the history entries are included in the search.
   *
   * @return the searchHistory
   */
  public boolean isSearchHistory() {
    return searchHistory;
  }

  /**
   * @param searchHistory the searchHistory to set
   * @return this for chaining.
   */
  public BaseSearchFilter setSearchHistory(final boolean searchHistory) {
    this.searchHistory = searchHistory;
    return this;
  }

  /**
   * If true, deleted and undeleted objects will be shown.
   */
  public boolean isIgnoreDeleted() {
    return ignoreDeleted;
  }

  /**
   * @param ignoreDeleted
   * @return this for chaining.
   */
  public BaseSearchFilter setIgnoreDeleted(final boolean ignoreDeleted) {
    this.ignoreDeleted = ignoreDeleted;
    return this;
  }

  /**
   * If not ignored, only deleted/undeleted object will be shown.
   */
  public boolean isDeleted() {
    return deleted;
  }

  /**
   * @param deleted
   * @return this for chaining.
   */
  public BaseSearchFilter setDeleted(final boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  /**
   * Maximum number of rows in the result list.
   *
   * @return
   */
  public int getMaxRows() {
    return maxRows;
  }

  /**
   * @param maxRows
   * @return this for chaining.
   */
  public BaseSearchFilter setMaxRows(final int maxRows) {
    this.maxRows = maxRows;
    return this;
  }

  /**
   * If true, the result set will be ordered limited by the data base query.
   *
   * @return
   */
  public boolean isSortAndLimitMaxRowsWhileSelect() {
    return sortAndLimitMaxRowsWhileSelect;
  }

  public void setSortAndLimitMaxRowsWhileSelect(boolean sortAndLimitMaxRowsWhileSelect) {
    this.sortAndLimitMaxRowsWhileSelect = sortAndLimitMaxRowsWhileSelect;
  }

  /**
   * @return the first sort order if available, otherwise null.
   */
  @Transient
  public SortOrder getSortOrder() {
    return CollectionUtils.isNotEmpty(sortProperties) ? sortProperties.get(0).getSortOrder() : SortOrder.ASCENDING;
  }

  /**
   * @return the first sort order if available, otherwise null.
   */
  @Transient
  public String getSortProperty() {
    return CollectionUtils.isNotEmpty(sortProperties) ? sortProperties.get(0).getProperty() : null;
  }

  public List<SortProperty> getSortProperties() {
    return sortProperties;
  }

  public void setSortProperties(List<SortProperty> sortProperties) {
    this.sortProperties = sortProperties;
  }

  /**
   * @param property
   * @return this for chaining.
   */
  public BaseSearchFilter setSortProperty(String property) {
    return setSortProperty(property, SortOrder.ASCENDING);
  }

  /**
   * @param property
   * @param sortOrder
   * @return this for chaining.
   */
  public BaseSearchFilter setSortProperty(String property, SortOrder sortOrder) {
    sortProperties = new ArrayList<>();
    sortProperties.add(new SortProperty(property, sortOrder));
    return this;
  }

  /**
   * @param property
   * @return this for chaining.
   */
  public BaseSearchFilter appendSortProperty(String property) {
    return appendSortProperty(property, SortOrder.ASCENDING);
  }

  /**
   * @param property
   * @param sortOrder
   * @return this for chaining.
   */
  public BaseSearchFilter appendSortProperty(String property, SortOrder sortOrder) {
    if (sortProperties == null)
      sortProperties = new ArrayList<>();
    sortProperties.add(new SortProperty(property, sortOrder));
    return this;
  }

  /**
   * If an error occured (e. g. lucene parse exception) this message will be returned.
   *
   * @return
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * @param errorMessage
   * @return this for chaining.
   */
  public BaseSearchFilter setErrorMessage(final String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  public boolean hasErrorMessage() {
    return StringUtils.isNotEmpty(errorMessage);
  }

  /**
   * @return this for chaining.
   */
  public BaseSearchFilter clearErrorMessage() {
    this.errorMessage = null;
    return this;
  }
}
