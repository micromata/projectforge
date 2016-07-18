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

package org.projectforge.plugins.skillmatrix;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * The list formular for the list view.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class TrainingListForm extends AbstractListForm<TrainingFilter, TrainingListPage> implements Serializable
{

  private static final long serialVersionUID = 1284459106693285166L;
  private static final Logger log = Logger.getLogger(TrainingListForm.class);

  @SpringBean
  private TrainingDao trainingDao;

  @SpringBean
  private SkillDao skillDao;

  /**
   * @param parentPage
   */
  public TrainingListForm(final TrainingListPage parentPage)
  {
    super(parentPage);
  }

  @SuppressWarnings("serial")
  @Override
  protected void init()
  {
    super.init();
    gridBuilder.newSplitPanel(GridSize.COL33);
    {
      // Skill
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.skillmatrix.skillrating.skill"));
      final SkillTextSelectPanel skillSelectPanel = new SkillTextSelectPanel(fs.newChildId(), new Model<SkillDO>()
      {
        @Override
        public SkillDO getObject()
        {
          return skillDao.getById(getSearchFilter().getSkillId());
        }

        @Override
        public void setObject(final SkillDO object)
        {
          if (object == null) {
            getSearchFilter().setSkillId(null);
          } else {
            getSearchFilter().setSkillId(object.getId());
          }
        }
      }, parentPage, "skillId");
      fs.add(skillSelectPanel);
      skillSelectPanel.setDefaultFormProcessing(false);
      skillSelectPanel.init().withAutoSubmit(true);
    }

    gridBuilder.newSplitPanel(GridSize.COL33);
    {
      // Training
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.skillmatrix.skilltraining.training"));
      final TrainingSelectPanel trainingSelectPanel = new TrainingSelectPanel(fs.newChildId(), new Model<TrainingDO>()
      {
        @Override
        public TrainingDO getObject()
        {
          return trainingDao.getById(getSearchFilter().getTrainingId());
        }

        @Override
        public void setObject(final TrainingDO object)
        {
          if (object == null) {
            getSearchFilter().setTrainingId(null);
          } else {
            getSearchFilter().setTrainingId(object.getId());
          }
        }
      }, parentPage, "trainingId");

      fs.add(trainingSelectPanel);
      trainingSelectPanel.setDefaultFormProcessing(false);
      trainingSelectPanel.init().withAutoSubmit(true);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#newSearchFilterInstance()
   */
  @Override
  protected TrainingFilter newSearchFilterInstance()
  {
    return new TrainingFilter();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
