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

import java.lang.reflect.Field;

import org.apache.commons.lang.ClassUtils;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.ChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.validation.validator.StringValidator;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.persistence.api.HibernateUtils;

public class MaxLengthTextField extends TextField<String>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MaxLengthTextField.class);

  private static final long serialVersionUID = -6577192527741433068L;

  @SuppressWarnings("rawtypes")
  private IConverter converter;

  /**
   * Tries to get the length definition of the Hibernate configuration. If not available then a warning will be logged.
   * Example:
   * 
   * <pre>
   * &lt;label wicket:id="streetLabel"&gt;[street]&lt;/&gt;&lt;input type="text" wicket:id="street" /&gt;<br/>
   * add(new MaxLengthTextField(this, "street", "address.street", model);
   * </pre>
   * 
   * @param id
   * @param model
   * @see org.apache.wicket.Component#Component(String, IModel)
   * @see FormComponent#setLabel(IModel)
   */
  public MaxLengthTextField(final String id, final IModel<String> model)
  {
    super(id, model);
    final Integer length = getMaxLength(model);
    init(id, length);
  }

  /**
   * Example:
   * 
   * <pre>
   * &lt;label wicket:id="streetLabel"&gt;[street]&lt;/&gt;&lt;input type="text" wicket:id="street" /&gt;<br/>
   * add(new MaxLengthTextField(this, "street", "address.street", model);
   * </pre>
   * 
   * @param parent if not null and label is not null than a label with wicket id [id]Label is added.
   * @param id
   * @param model
   * @param maxLength
   * @see org.apache.wicket.Component#Component(String, IModel)
   * @see FormComponent#setLabel(IModel)
   */
  public MaxLengthTextField(final String id, final IModel<String> model, final int maxLength)
  {
    super(id, model);
    final Integer length = getMaxLength(model, maxLength);
    init(id, length);
  }

  private void init(final String id, final Integer maxLength)
  {
    if (maxLength != null && maxLength != -1) {
      add(StringValidator.maximumLength(maxLength));
      // add(AttributeModifier.replace("maxlength", String.valueOf(maxLength))); // Field maxlength is produced by StringValidator.
    }
  }

  /**
   * @see org.apache.wicket.Component#getConverter(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <C> IConverter<C> getConverter(final Class<C> type)
  {
    if (converter != null) {
      return converter;
    } else {
      return super.getConverter(type);
    }
  }

  /**
   * Setting a converter is more convenient instead of overriding method getConverter(Class).
   * 
   * @param converter
   * @return This for chaining.
   */
  public <C> MaxLengthTextField setConverter(final IConverter<C> converter)
  {
    this.converter = converter;
    return this;
  }

  /**
   * The field length (if defined by Hibernate). The entity is the target class of the PropertyModel and the field name
   * is the expression of the given PropertyModel.
   * 
   * @param model If not from type PropertyModel then null is returned.
   * @return
   */
  public static Integer getMaxLength(final IModel<String> model)
  {
    Integer length = null;
    if (ClassUtils.isAssignable(model.getClass(), PropertyModel.class)) {
      final PropertyModel<?> propertyModel = (PropertyModel<?>) model;
      final Object entity = BeanHelper.getFieldValue(propertyModel, ChainingModel.class, "target");
      if (entity == null) {
        log.warn("Oups, can't get private field 'target' of PropertyModel!.");
      } else {
        final Field field = propertyModel.getPropertyField();
        if (field != null) {
          length = HibernateUtils.getPropertyLength(entity.getClass().getName(), field.getName());
        } else {
          log.info("Can't get field '" + propertyModel.getPropertyExpression() + "'.");
        }
      }
    }
    return length;
  }

  /**
   * The field length (if defined by Hibernate). The entity is the target class of the PropertyModel and the field name
   * is the expression of the given PropertyModel.
   * 
   * @param model If not from type PropertyModel then null is returned.
   * @return
   */
  public static Integer getMaxLength(final IModel<String> model, final Integer maxLength)
  {
    final Integer dbLength = getMaxLength(model);
    if (dbLength != null) {
      if (maxLength != null) {
        if (dbLength < maxLength) {
          log.warn("Data base length of given property is less than given maxLength: " + model);
        } else if (dbLength > maxLength) {
          return maxLength;
        }
      }
      return dbLength;
    }
    return maxLength;
  }
}
