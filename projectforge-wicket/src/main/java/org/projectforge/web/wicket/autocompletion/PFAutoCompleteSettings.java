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

package org.projectforge.web.wicket.autocompletion;

import java.io.Serializable;

public class PFAutoCompleteSettings implements Serializable
{
  private static final long serialVersionUID = 5147919187217570137L;

  private Boolean matchContains;

  private Integer minChars;

  private Integer delay;

  private Boolean matchCase;

  private Boolean matchSubset;

  private Integer cacheLength;

  private Boolean mustMatch;

  private Boolean selectFirst;

  private Boolean selectOnly;

  private Integer maxItemsToShow;

  private Boolean autoFill;

  private Integer width;

  private Boolean autoSubmit;

  private Boolean scroll;

  private Integer scrollHeight;

  private boolean hasFocus;

  private boolean labelValue;

  private boolean deletableItem;

  public Boolean isMatchContains()
  {
    return matchContains;
  }

  public PFAutoCompleteSettings withMatchContains(final boolean matchContains)
  {
    this.matchContains = matchContains;
    return this;
  }

  /**
   * Minimum characters to enter before ajax autocompletion starts. Default in java script is 2.
   */
  public Integer getMinChars()
  {
    return minChars;
  }

  /** Fluent. */
  public PFAutoCompleteSettings withMinChars(final int minChars)
  {
    this.minChars = minChars;
    return this;
  }

  /**
   * Delay time in ms before ajax call starts. Default in java script is 200.
   */
  public Integer getDelay()
  {
    return delay;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withDelay(final int delay)
  {
    this.delay = delay;
    return this;
  }

  /**
   * Case sensitive or not. Default in java script is false.
   */
  public Boolean isMatchCase()
  {
    return matchCase;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withMatchCase(final boolean matchCase)
  {
    this.matchCase = matchCase;
    return this;
  }

  /**
   * Match subset or not (if true then e. g. 'asse' matches for 'Kassel'. Default in java script is true.
   */
  public Boolean isMatchSubset()
  {
    return matchSubset;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withMatchSubset(final boolean matchSubset)
  {
    this.matchSubset = matchSubset;
    return this;
  }

  /**
   * Show scrollbar or not. Default in java script is true.
   */
  public Boolean isScroll()
  {
    return scroll;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withScroll(final boolean scroll)
  {
    this.scroll = scroll;
    return this;
  }

  /**
   * Height of box in px. Default in java script is 400.
   */
  public Integer getScrollHeight()
  {
    return scrollHeight;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withScrollHeight(final int scrollHeight)
  {
    this.scrollHeight = scrollHeight;
    return this;
  }

  /**
   * If true, fills in the input box w/the first match (assumed to be the best match). Default in java script is false.
   */
  public Boolean isAutoFill()
  {
    return autoFill;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withAutoFill(final boolean autoFill)
  {
    this.autoFill = autoFill;
    return this;
  }

  /**
   * If true then form will be submitted after selection of an entry. Default in java script is false.
   */
  public Boolean isAutoSubmit()
  {
    return autoSubmit;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withAutoSubmit(final boolean autoSubmit)
  {
    this.autoSubmit = autoSubmit;
    return this;
  }

  /**
   * Default in java script is false.
   */
  public Boolean isMustMatch()
  {
    return mustMatch;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withMustMatch(final boolean mustMatch)
  {
    this.mustMatch = mustMatch;
    return this;
  }

  /**
   * If true then the first entry will be pre-selected. Default in java script is false.
   */
  public Boolean isSelectFirst()
  {
    return selectFirst;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withSelectFirst(final boolean selectFirst)
  {
    this.selectFirst = selectFirst;
    return this;
  }

  /**
   * If true then an only entry will be pre-selected. Default in java script is false.
   */
  public Boolean isSelectOnly()
  {
    return selectOnly;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withSelectOnly(final boolean selectOnly)
  {
    this.selectOnly = selectOnly;
    return this;
  }

  /**
   * Default in java script is unlimited.
   */
  public Integer getMaxItemsToShow()
  {
    return maxItemsToShow;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withMaxItemsToShow(final int maxItemsToShow)
  {
    this.maxItemsToShow = maxItemsToShow;
    return this;
  }

  /**
   * The size of the cache. Default in java script is 1.
   */
  public Integer getCacheLength()
  {
    return cacheLength;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withCacheLength(final int cacheLength)
  {
    this.cacheLength = cacheLength;
    return this;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withWidth(final int width)
  {
    this.width = width;
    return this;
  }

  /**
   * Width of drop down choice box.
   */
  public Integer getWidth()
  {
    return width;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withFocus(final boolean hasFocus)
  {
    this.hasFocus = hasFocus;
    return this;
  }

  /**
   * If true then focus will be set on this field of the form.
   * @return
   */
  public boolean isHasFocus()
  {
    return hasFocus;
  }

  /**
   * Fluent.
   */
  public PFAutoCompleteSettings withLabelValue(final boolean labelValue)
  {
    this.labelValue = labelValue;
    return this;
  }

  /**
   * @return True, if json data contains rows where the first column represents the label (shown in the drop down list) and the second
   *         column the value to fill in the input text field after selection.
   */
  public boolean isLabelValue()
  {
    return labelValue;
  }

  /**
   * @return the deletableItem
   */
  public boolean isDeletableItem()
  {
    return deletableItem;
  }

  /**
   * @param deletableItem the deletableItem to set
   * @return this for chaining.
   */
  public PFAutoCompleteSettings setDeletableItem(final boolean deletableItem)
  {
    this.deletableItem = deletableItem;
    return this;
  }

}
