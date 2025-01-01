/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common.props;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.commons.test.TestUtils;

public class PropUtilsTest {

    private final String i18nKey = "prop.test";

    @Test
    public void testGet() throws Exception {
        Object o = new Object();
        Assertions.assertNull(PropUtils.get(o.getClass(), "noClass"));
        Integer integer = Integer.valueOf(42);
        Assertions.assertNull(PropUtils.get(integer.getClass().getDeclaredField("value")));

        TestProp testProp = new TestProp();
        Assertions.assertNotNull(PropUtils.get(TestProp.class, "property"));

        Assertions.assertNull(PropUtils.get(null));

    }

    @Test
    public void getField() {
        Integer integer = Integer.valueOf(12);

        Assertions.assertNotNull(PropUtils.getField(Integer.class, "value"));
        Assertions.assertNull(PropUtils.getField(Object.class, "value"));
        Assertions.assertNotNull(PropUtils.getField(TestProp.class, "property"));
    }

    @Test
    public void getNestedField() {
        Assertions.assertNotNull(PropUtils.getField(Timesheet.class, "task.id"));
        Assertions.assertNull(PropUtils.getField(Timesheet.class, "task.notThere"));
    }

    @Test
    public void testGetI18NKey() {
        TestUtils.suppressErrorLogs(() -> {
            Assertions.assertNull(PropUtils.getI18nKey(Integer.class, "class"));
            return null;
        });
        Assertions.assertEquals(PropUtils.getI18nKey(TestProp.class, "property"), i18nKey);
    }

    @Test
    public void testGetPropertyInfoFields() {
        Assertions.assertEquals(PropUtils.getPropertyInfoFields(TestProp.class).length, 1);
        Assertions.assertEquals(PropUtils.getPropertyInfoFields(TestProp.class).length, 1);
        Assertions.assertEquals(PropUtils.getPropertyInfoFields(Object.class).length, 0);
    }

    class TestProp {
        @PropertyInfo(i18nKey = i18nKey)
        private String property;

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }

    class Task {
        Integer id;
    }

    class Timesheet {
        Task task;
    }
}
