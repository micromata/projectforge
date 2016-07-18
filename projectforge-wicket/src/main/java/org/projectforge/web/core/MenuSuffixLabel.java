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

package org.projectforge.web.core;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MenuSuffixLabel extends Label
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MenuSuffixLabel.class);

  private Integer counter;

  private final IModel<Integer> counterModel;

  private static final long serialVersionUID = 3239158008743709277L;

  public MenuSuffixLabel(final IModel<Integer> counterModel)
  {
    this("suffix", counterModel);
  }

  @SuppressWarnings("serial")
  public MenuSuffixLabel(final String id, final IModel<Integer> counterModel)
  {
    super(id);
    this.counterModel = counterModel;
    setDefaultModel(new Model<String>() {
      @Override
      public String getObject()
      {
        return getCounterValue();
      }
    });
  }

  private String getCounterValue()
  {
    if (counterModel == null) {
      return "";
    }
    if (counter == null) {
      try {
        counter = counterModel.getObject();
      } catch (final Throwable ex) {
        log.error(ex.getMessage(), ex);
      }
    }
    if (counter != null && counter > 0) {
      return String.valueOf(counter);
    } else {
      return "";
    }
  }

  @Override
  protected void onBeforeRender()
  {
    counter = null; // Force recalculation.
    super.onBeforeRender();
  }

  @Override
  public boolean isVisible()
  {
    if (counter == null) {
      getCounterValue();
    }
    return counter != null && counter > 0;
  }
}
