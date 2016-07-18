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

package org.projectforge.web.wicket.flowlayout;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * For sharing functionality (refer {@link org.projectforge.web.address.AddressPageSupport} as an example).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class FieldProperties<T>
{
	private final String label;

	private String labelDescription;

	private boolean translateLabelDecsription = true;

	private final IModel<T> model;

	private FieldType fieldType;

	private String valueAsString;

	public FieldProperties(final String label, final IModel<T> model)
	{
		this.label = label;
		this.model = model;
	}

	/**
	 * @return the label (i18n key at default).
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * @return the model
	 */
	public IModel<T> getModel()
	{
		return model;
	}

	/**
	 * @return the model which will be cast to PropertyModel<T> first.
	 * @throws ClassCastException if the model isn't an instance of {@link PropertyModel}.
	 */
	public PropertyModel<T> getPropertyModel()
	{
		return (PropertyModel<T>) model;
	}

	/**
	 * @return the fieldType
	 */
	public FieldType getFieldType()
	{
		return fieldType;
	}

	/**
	 * @param fieldType the fieldType to set
	 * @return this for chaining.
	 */
	public FieldProperties<T> setFieldType(final FieldType fieldType)
	{
		this.fieldType = fieldType;
		return this;
	}

	public T getValue()
	{
		return model.getObject();
	}

	/**
	 * @return the valueAsString
	 */
	public String getValueAsString()
	{
		return valueAsString;
	}

	/**
	 * @param valueAsString the valueAsString to set
	 * @return this for chaining.
	 */
	public FieldProperties<T> setValueAsString(final String valueAsString)
	{
		this.valueAsString = valueAsString;
		return this;
	}

	/**
	 * Is only supported by FieldsetPanel (in desktop version).
	 * @return the labelDescription
	 */
	public String getLabelDescription()
	{
		return labelDescription;
	}

	/**
	 * @param labelDescription the labelDescription to set
	 * @return this for chaining.
	 */
	public FieldProperties<T> setLabelDescription(final String labelDescription)
	{
		return setLabelDescription(labelDescription, true);
	}

	/**
	 * @param labelDescription the labelDescription to set
	 * @param translate if true (default) then the label description is an i18n key which has to be translated.
	 * @return this for chaining.
	 */
	public FieldProperties<T> setLabelDescription(final String labelDescription, final boolean translate)
	{
		this.labelDescription = labelDescription;
		this.translateLabelDecsription = translate;
		return this;
	}

	/**
	 * If true (default) then the label description is an i18n key which has to be translated.
	 * @return the translateLabelDecsription
	 */
	public boolean isTranslateLabelDecsription()
	{
		return translateLabelDecsription;
	}
}
