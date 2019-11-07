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

package org.projectforge.plugins.memo;

import org.apache.wicket.model.PropertyModel;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.AbstractEditForm;
import org.projectforge.web.wicket.WicketUtils;
import org.projectforge.web.wicket.components.MaxLengthTextArea;
import org.projectforge.web.wicket.components.RequiredMaxLengthTextField;
import org.projectforge.web.wicket.flowlayout.FieldsetPanel;
import org.slf4j.Logger;

/**
 * This is the edit formular page.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MemoEditForm extends AbstractEditForm<MemoDO, MemoEditPage>
{
  private static final long serialVersionUID = -6208809585214296635L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MemoEditForm.class);

  public MemoEditForm(final MemoEditPage parentPage, final MemoDO data)
  {
    super(parentPage, data);
    data.setOwner(ThreadLocalUserContext.getUser());
  }

  @Override
  protected void init()
  {
    super.init();
    {
      // Subject
      final FieldsetPanel fs = gridBuilder.newFieldset(MemoDO.class, "subject");
      final RequiredMaxLengthTextField subject = new RequiredMaxLengthTextField(fs.getTextFieldId(),
          new PropertyModel<>(data, "subject"));
      subject.add(WicketUtils.setFocus());
      fs.add(subject);
    }
    {
      // Text description
      final FieldsetPanel fs = gridBuilder.newFieldset(MemoDO.class, "memo");
      fs.add(new MaxLengthTextArea(fs.getTextAreaId(), new PropertyModel<>(data, "memo"))).setAutogrow();
    }
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }
}
