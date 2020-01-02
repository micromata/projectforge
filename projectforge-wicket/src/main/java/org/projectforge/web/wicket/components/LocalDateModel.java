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

package org.projectforge.web.wicket.components;

import org.apache.wicket.model.IModel;
import org.projectforge.framework.time.PFDay;

import java.util.Date;
import java.time.LocalDate;

/**
 * https://stackoverflow.com/questions/39162334/how-can-i-bring-wicket-7-to-work-with-java-time-from-java-8
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class LocalDateModel implements IModel<Date> {
  private IModel<LocalDate> model;

  public LocalDateModel(IModel<LocalDate> localDateModel){
    this.model = localDateModel;
  }

  @Override
  public Date getObject() {
    return PFDay.from(model.getObject()).getSqlDate();
  }

  @Override
  public void setObject(Date object) {
    model.setObject(PFDay.from(object).getDate());
  }

  @Override
  public void detach() {
    model.detach();
  }
}
