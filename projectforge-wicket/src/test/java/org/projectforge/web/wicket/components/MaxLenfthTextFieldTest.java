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

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.junit.jupiter.api.Test;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.test.AbstractTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class MaxLenfthTextFieldTest extends AbstractTestBase {
  @Test
  public void maxLength() {
    PropertyModel<String> model = new PropertyModel<String>(new PFUserDO(), "username");
    assertInteger(255, MaxLengthTextField.getMaxLength(model));
    assertInteger(255, MaxLengthTextField.getMaxLength(model, 300));
    assertInteger(100, MaxLengthTextField.getMaxLength(model, 100));

    model = new PropertyModel<String>(new BaseSearchFilter(), "searchString");
    assertNull(MaxLengthTextField.getMaxLength(model));
    assertInteger(100, MaxLengthTextField.getMaxLength(model, 100));

    assertNull(MaxLengthTextField.getMaxLength(Model.of("test")));
    assertInteger(100, MaxLengthTextField.getMaxLength(Model.of("test"), 100));
  }

  private void assertInteger(final int expectedValue, final Integer value) {
    assertNotNull(value);
    assertEquals(expectedValue, value.intValue());
  }
}
