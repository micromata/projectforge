package org.projectforge.export;

import java.lang.reflect.Field;

import org.projectforge.common.BeanHelper;
import org.projectforge.excel.PropertyMapping;

/**
 * Created by blumenstein on 24.11.16.
 */
public class DOGetterListExcelExporter extends DOListExcelExporter
{
  public DOGetterListExcelExporter(final String filenameIdentifier)
  {
    super(filenameIdentifier);
  }

  @Override
  public void addMapping(final PropertyMapping mapping, final Object entry, final Field field)
  {
    if (BeanHelper.getFieldValue(entry, field) != null) {
      mapping.add(field.getName(), BeanHelper.getFieldValue(entry, field));
    } else {
      Object result = BeanHelper.invoke(entry, BeanHelper.determineGetter(entry.getClass(), field.getName()));
      if (result != null) {
        mapping.add(field.getName(), result.toString());
      }

    }
  }

}
