package org.projectforge.framework.persistence.jpa;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.projectforge.business.fibu.EmployeeDO;
import org.projectforge.framework.persistence.api.IdObject;
import org.junit.jupiter.api.Test;

import de.micromata.genome.jpa.DbRecord;
import de.micromata.genome.util.bean.FieldMatchers;
import de.micromata.genome.util.bean.PrivateBeanUtils;
import de.micromata.genome.util.matcher.CommonMatchers;
import de.micromata.genome.util.matcher.Matcher;

public class BaseDaoJpaAdapterTest
{
  @Test
  public void testFindFields()
  {
    Class<?> srcClazz = EmployeeDO.class;
    Matcher<Field> matcher = CommonMatchers.and(
        FieldMatchers.hasNotModifier(Modifier.STATIC),
        FieldMatchers.hasNotModifier(Modifier.TRANSIENT),
        CommonMatchers.not(
            CommonMatchers.or(
                FieldMatchers.assignableTo(Collection.class),
                FieldMatchers.assignableTo(Map.class),
                FieldMatchers.assignableTo(DbRecord.class),
                FieldMatchers.assignableTo(IdObject.class))));
    List<Field> foundFields = PrivateBeanUtils.findAllFields(srcClazz, matcher);
    foundFields.size();
  }
}
