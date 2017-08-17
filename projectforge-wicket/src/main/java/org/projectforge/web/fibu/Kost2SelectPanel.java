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

package org.projectforge.web.fibu;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.fibu.kost.Kost2DO;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.web.wicket.AbstractSelectPanel;
import org.projectforge.web.wicket.WebConstants;
import org.projectforge.web.wicket.components.TooltipImage;

/**
 * This panel show the actual kost2 and buttons for select/unselect kost2s.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class Kost2SelectPanel extends AbstractSelectPanel<Kost2DO>
{
  private static final long serialVersionUID = 1017253478360068965L;

  @SpringBean
  private Kost2Dao kost2Dao;

  public Kost2SelectPanel(final String id, final IModel<Kost2DO> model, final ISelectCallerPage caller, final String selectProperty)
  {
    super(id, model, caller, selectProperty);
  }

  @Override
  @SuppressWarnings("serial")
  public Kost2SelectPanel init()
  {
    super.init();
    final Label kost2AsStringLabel = new Label("kost2AsString", new Model<String>()
    {

      @Override
      public String getObject()
      {
        final Kost2DO kost2 = getModelObject();
        if (kost2 == null) {
          return "";
        }
        return kost2.getFormattedNumber();
      }
    });
    add(kost2AsStringLabel);
    final SubmitLink selectButton = new SubmitLink("select")
    {
      @Override
      public void onSubmit()
      {
        final PageParameters parameters = new PageParameters();
        beforeSelectPage(parameters);
        final Kost2ListPage page = new Kost2ListPage(parameters, caller, selectProperty);
        setResponsePage(page);
      }
    };
    selectButton.setDefaultFormProcessing(false);
    add(selectButton);
    final boolean hasSelectAccess = kost2Dao.hasLoggedInUserSelectAccess(false);
    if (hasSelectAccess == false) {
      selectButton.setVisible(false);
    }
    selectButton.add(new TooltipImage("selectHelp", WebConstants.IMAGE_KOST2_SELECT, getString("fibu.tooltip.selectKost2")));
    final SubmitLink unselectButton = new SubmitLink("unselect")
    {
      @Override
      public void onSubmit()
      {
        caller.unselect(selectProperty);
      }

      @Override
      public boolean isVisible()
      {
        return hasSelectAccess == true && isRequired() == false && getModelObject() != null;
      }
    };
    unselectButton.setDefaultFormProcessing(false);
    add(unselectButton);
    unselectButton.add(new TooltipImage("unselectHelp", WebConstants.IMAGE_KOST2_UNSELECT, getString("fibu.tooltip.unselectKost2")));
    return this;
  }

  protected void beforeSelectPage(final PageParameters parameters)
  {
  }

  @Override
  public void convertInput()
  {
    setConvertedInput(getModelObject());
  }
}
