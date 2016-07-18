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

package org.projectforge.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.projectforge.framework.utils.NumberHelper;

/**
 * For displaying the total number of new items as sum of all counters from the sub menu entries.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class TotalNewCounterModel extends Model<Integer>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TotalNewCounterModel.class);

  private static final long serialVersionUID = -900845361698793144L;

  private final List<IModel<Integer>> models = new ArrayList<IModel<Integer>>();

  public void add(final IModel<Integer> model)
  {
    models.add(model);
  }

  @Override
  public Integer getObject()
  {
    if (models == null) {
      return null;
    }
    Integer totalCounter = 0;
    for (final IModel<Integer> model : models) {
      try {
        final Integer counter = model.getObject();
        if (NumberHelper.greaterZero(counter) == true) {
          totalCounter += counter;
        }
      } catch (final Throwable ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    if (NumberHelper.greaterZero(totalCounter) == false) {
      return null;
    }
    return totalCounter;
  }
}
