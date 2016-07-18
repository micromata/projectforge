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
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.web.user.UserSelectPanel;
import org.projectforge.web.wicket.AbstractListForm;
import org.projectforge.web.wicket.bootstrap.GridSize;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;

/**
 * The list formular for the list view.
 * 
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class TrainingAttendeeListForm extends AbstractListForm<TrainingAttendeeFilter, TrainingAttendeeListPage>
    implements Serializable
{

  private static final long serialVersionUID = 314512845221133499L;

  private static final Logger log = Logger.getLogger(TrainingAttendeeListForm.class);

  @SpringBean
  private TrainingDao trainingDao;

  @SpringBean
  private SkillDao skillDao;

  /**
   * @param parentPage
   */
  public TrainingAttendeeListForm(final TrainingAttendeeListPage parentPage)
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

    gridBuilder.newSplitPanel(GridSize.COL66);
    {
      // Attendee
      final FieldsetPanel fs = gridBuilder.newFieldset(getString("plugins.skillmatrix.skilltraining.attendee.menu"));
      final UserSelectPanel attendeeSelectPanel = new UserSelectPanel(fs.newChildId(), new Model<PFUserDO>()
      {
        @Override
        public PFUserDO getObject()
        {
          return getTenantRegistry().getUserGroupCache().getUser(getSearchFilter().getAttendeeId());
        }

        @Override
        public void setObject(final PFUserDO object)
        {
          if (object == null) {
            getSearchFilter().setAttendeeId(null);
          } else {
            getSearchFilter().setAttendeeId(object.getId());
          }
        }
      }, parentPage, "attendeeId");
      fs.add(attendeeSelectPanel);
      attendeeSelectPanel.setDefaultFormProcessing(false);
      attendeeSelectPanel.init().withAutoSubmit(true);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractListForm#newSearchFilterInstance()
   */
  @Override
  protected TrainingAttendeeFilter newSearchFilterInstance()
  {
    return new TrainingAttendeeFilter();
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
