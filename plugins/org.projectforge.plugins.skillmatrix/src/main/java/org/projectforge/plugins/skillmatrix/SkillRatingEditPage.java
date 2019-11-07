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

package org.projectforge.plugins.skillmatrix;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.utils.NumberHelper;
import org.projectforge.web.fibu.ISelectCallerPage;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.WicketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Billy Duong (b.duong@micromata.de)
 * 
 */
public class SkillRatingEditPage extends AbstractEditPage<SkillRatingDO, SkillRatingEditForm, SkillRatingDao>
    implements ISelectCallerPage
{
  private static final long serialVersionUID = 1403978551875901644L;

  private static final Logger log = LoggerFactory.getLogger(SkillRatingEditPage.class);

  public static final String I18N_KEY_PREFIX = "plugins.skillmatrix.rating";

  @SpringBean
  private SkillRatingDao skillRatingDao;

  Integer skillId = -1;

  /**
   * @param parameters
   */
  public SkillRatingEditPage(final PageParameters parameters)
  {
    super(parameters, I18N_KEY_PREFIX);
    skillId = WicketUtils.getAsInteger(parameters, SkillRatingEditForm.PARAM_SKILL_ID);
    init();
    if (NumberHelper.greaterZero(skillId)) {
      skillRatingDao.setSkill(getData(), skillId);
    }
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getBaseDao()
   */
  @Override
  protected SkillRatingDao getBaseDao()
  {
    return skillRatingDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#newEditForm(org.projectforge.web.wicket.AbstractEditPage,
   *      org.projectforge.core.AbstractBaseDO)
   */
  @Override
  protected SkillRatingEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final SkillRatingDO data)
  {
    return new SkillRatingEditForm(this, data);
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#select(java.lang.String, java.lang.Object)
   */
  @Override
  public void select(final String property, final Object selectedValue)
  {
    if ("skillId".equals(property)) {
      skillRatingDao.setSkill(getData(), (Integer) selectedValue);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#unselect(java.lang.String)
   */
  @Override
  public void unselect(final String property)
  {
    if ("skillId".equals(property)) {
      getData().setSkill(null);
    } else {
      log.error("Property '" + property + "' not supported for selection.");
    }
  }

  /**
   * @see org.projectforge.web.fibu.ISelectCallerPage#cancelSelection(java.lang.String)
   */
  @Override
  public void cancelSelection(final String property)
  {
    // Do nothing
  }

}
