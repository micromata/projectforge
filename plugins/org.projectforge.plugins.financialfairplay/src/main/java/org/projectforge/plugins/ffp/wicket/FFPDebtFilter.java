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

package org.projectforge.plugins.ffp.wicket;

import java.io.Serializable;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Florian Blumenstein
 */
@XStreamAlias("FFPDebtFilter")
public class FFPDebtFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = 8567780910637887786L;

  private Integer userId;
 
  private boolean fromMe; 
  
  private boolean toMe; 
  
  // show only debts i need to approve
  private boolean iNeedToApprove=true; 

  // hide if both approved already 
  private boolean hideBothApproved=true; 
 

  public FFPDebtFilter(Integer userId)
  {
    this.userId = userId;
  }

  public FFPDebtFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public Integer getUserId()
  {
    return userId;
  }

  public void setUserId(Integer userId)
  {
    this.userId = userId;
  }

  public boolean isFromMe() {
    return fromMe;
  }

  public void setFromMe(boolean fromMe) {
  this.fromMe = fromMe;
  }

  public boolean isToMe() {
  return toMe;
  }

  public void setToMe(boolean toMe) {
    this.toMe = toMe;
  }

  public boolean isiNeedToApprove() {
    return iNeedToApprove;
  }

  public void setiNeedToApprove(boolean iNeedToApprove) {
    this.iNeedToApprove = iNeedToApprove;
  }

  public boolean isHideBothApproved() {
    return hideBothApproved;
  }

  public void setHideBothApproved(boolean hideBothApproved) {
    this.hideBothApproved = hideBothApproved;
  }
}
