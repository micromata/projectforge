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

package org.projectforge.framework.persistence.attr.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

import de.micromata.genome.db.jpa.tabattr.api.EntityWithAttributes;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;
import de.micromata.genome.jpa.ComplexEntity;
import de.micromata.genome.jpa.ComplexEntityVisitor;
import de.micromata.genome.util.strings.converter.StandardStringConverter;
import de.micromata.genome.util.strings.converter.StringConverter;
import de.micromata.genome.util.types.Pair;

/**
 * @author Florian Blumenstein (f.blumenstein@micromata.de)
 * @param <M>
 */
@MappedSuperclass
public abstract class DefaultBaseWithAttrDO<M extends DefaultBaseWithAttrDO<?>>extends DefaultBaseDO
    implements EntityWithAttributes, ComplexEntity
{
  private static final long serialVersionUID = 1L;

  private StringConverter stringConverter = StandardStringConverter.get();

  /**
   * holds the attributes
   */
  private Map<String, JpaTabAttrBaseDO<M, Integer>> attributes = new TreeMap<String, JpaTabAttrBaseDO<M, Integer>>();

  public DefaultBaseWithAttrDO()
  {

  }

  public DefaultBaseWithAttrDO(final StringConverter stringConverter)
  {
    this.stringConverter = stringConverter;
  }

  /**
   * Entity for the an attribute, where the value fits into the AttrBaseDO.value field.
   *
   * @return
   */
  @Transient
  public abstract Class<? extends JpaTabAttrBaseDO<M, Integer>> getAttrEntityClass();

  /**
   * Entity for the an attribute, where the value does NOT fit into the AttrBaseDO.value field, but need nested
   * AttrDataBaseDO
   *
   * @return
   */
  @Transient
  public abstract Class<? extends JpaTabAttrBaseDO<M, Integer>> getAttrEntityWithDataClass();

  /**
   * Entity class for the AttrData table.
   *
   * @return
   */
  @Transient
  public abstract Class<? extends JpaTabAttrDataBaseDO<? extends JpaTabAttrBaseDO<M, Integer>, Integer>> getAttrDataEntityClass();

  /**
   * Create a new attribute entity where the value fits into the AttrBaseDO.value field.
   *
   * The constructor of the entity has to pass it to the constructor AttrBaseDO(M parent, String propertyName, String
   * value)
   *
   * @param key AttrBaseDO.propertyName
   * @param value AttrBaseDO.value
   * @return new entity
   */
  public abstract JpaTabAttrBaseDO<M, Integer> createAttrEntity(String key, char type, String value);

  /**
   * Create a new attribute entity where the value does NOT fit into the AttrBaseDO.value field.
   *
   * The constructor of the entity has to pass it to the constructor AttrBaseDO(M parent, String propertyName, String
   * value)
   *
   * @param key AttrBaseDO.propertyName
   * @param value AttrBaseDO.value
   * @return new entity
   */
  public abstract JpaTabAttrBaseDO<M, Integer> createAttrEntityWithData(String key, char type, String value);

  @Override
  public void visit(ComplexEntityVisitor visitor)
  {
    visitor.visit(this);
    for (JpaTabAttrBaseDO<M, Integer> attr : attributes.values()) {
      attr.visit(visitor);
    }
  }

  /**
   * Get a attribute from entity
   *
   * @param key must not be null
   * @return null, if attribute is not defined. Otherwise value stored
   */
  @Override
  @Transient
  public String getStringAttribute(final String key)
  {
    final JpaTabAttrBaseDO<M, Integer> tabr = getAttributeRow(key);
    if (tabr == null) {
      return null;
    }
    return tabr.getValue();
  }

  /**
   * Put an attribute identified by key into the attribute map.
   *
   * @param key must not be null
   * @param value must not be null
   */
  @Override
  public void putStringAttribute(final String key, final String value)
  {
    putAttrInternal(key, stringConverter.getTypeChar(value), value);
  }

  /*
   * (non-Javadoc)
   *
   * @see de.micromata.genome.jpa.EntityWithAttributes#getAttribute(java.lang.String)
   */
  @Override
  public Object getAttribute(final String key)
  {
    final JpaTabAttrBaseDO<M, Integer> tabr = getAttributeRow(key);
    if (tabr == null) {
      return null;
    }
    final String data = tabr.getStringData();
    return stringConverter.stringToObject(tabr.getType(), data);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getAttribute(final String key, final Class<T> expectedClass)
  {
    final Object val = getAttribute(key);
    if (val == null) {
      return null;
    }
    if (expectedClass.isAssignableFrom(val.getClass()) == false) {
      throw new IllegalArgumentException("Attribute does not match type. key: "
          + key
          + "; expected: "
          + expectedClass.getName()
          + "; retreived: "
          + val.getClass().getName());
    }
    return (T) val;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.micromata.genome.jpa.EntityWithAttributes#putAttribute(java.lang.String, java.lang.Object)
   */
  @Override
  public void putAttribute(final String key, final Object value)
  {
    final Pair<Character, String> p = stringConverter.objectToString(value);
    putAttrInternal(key, p.getFirst(), p.getSecond());
  }

  public void putAttrInternal(final String key, final Character type, final String encodedString)
  {

    final JpaTabAttrBaseDO<M, Integer> tabr = getAttributeRow(key);
    final Class<?> required = StringUtils.length(encodedString) > JpaTabAttrDataBaseDO.DATA_MAXLENGTH
        ? getAttrEntityWithDataClass()
        : getAttrEntityClass();

    if (tabr != null && required == tabr.getClass()) {
      tabr.setStringData(encodedString);
      tabr.setType(type);
      return;
    }

    if (StringUtils.length(encodedString) > JpaTabAttrDataBaseDO.DATA_MAXLENGTH) {
      putAttributeRow(key, createAttrEntityWithData(key, type, encodedString));
    } else {
      putAttributeRow(key, createAttrEntity(key, type, encodedString));
    }
  }

  /**
   * removes the attribute.
   *
   * @param key aka propertyName
   */
  @Override
  public void removeAttribute(final String key)
  {
    attributes.remove(key);
  }

  /**
   * The keys of the attributes.
   *
   * @return never null
   */
  @Override
  @Transient
  public Set<String> getAttributeKeys()
  {
    return attributes.keySet();
  }

  //  @Override
  //  public void visit(final ComplexEntityVisitor visitor)
  //  {
  //    visitor.visit(this);
  //    for (final AttrBaseDO<M> d : getAttributes().values()) {
  //      d.visit(visitor);
  //    }
  //  }

  @Override
  public ModificationStatus copyValuesFrom(final BaseDO<? extends Serializable> source, final String... ignoreFields)
  {
    String[] combinedIgnoreFields = Stream
        .concat(Stream.of(ignoreFields), Stream.of("attributes"))
        .toArray(String[]::new);

    ModificationStatus modificationStatus = super.copyValuesFrom(source, combinedIgnoreFields);
    final DefaultBaseWithAttrDO<M> src = (DefaultBaseWithAttrDO) source;

    for (final Map.Entry<String, JpaTabAttrBaseDO<M, Integer>> srcEntry : src.getAttrs().entrySet()) {
      final JpaTabAttrBaseDO<M, Integer> destEntry = getAttributeRow(srcEntry.getKey());
      if (destEntry == null || destEntry.getClass() != srcEntry.getValue().getClass()) {
        removeAttribute(srcEntry.getKey());
        // create new entry if either none exists, or with/out data is different.
        putAttrInternal(srcEntry.getKey(), srcEntry.getValue().getType(), srcEntry.getValue().getStringData());
        modificationStatus = getModificationStatus(modificationStatus, ModificationStatus.MAJOR);
      } else {
        if (StringUtils.equals(destEntry.getStringData(), srcEntry.getValue().getStringData()) == false) {
          removeAttribute(srcEntry.getKey());
          modificationStatus = modificationStatus.combine(ModificationStatus.MAJOR);
          putAttrInternal(srcEntry.getKey(), srcEntry.getValue().getType(), srcEntry.getValue().getStringData());
        }
      }

    }
    final ArrayList<String> keys = new ArrayList<>();
    keys.addAll(attributes.keySet());
    for (final String key : keys) {
      if (src.getAttrs().containsKey(key) == false) {
        removeAttribute(key);
        modificationStatus = getModificationStatus(modificationStatus, ModificationStatus.MAJOR);
      }
    }
    return modificationStatus;

  }

  public JpaTabAttrBaseDO<M, Integer> getAttributeRow(final String key)
  {
    return attributes.get(key);
  }

  public void putAttributeRow(final String key, final JpaTabAttrBaseDO<M, Integer> attr)
  {
    attributes.put(key, attr);
  }

  @Transient
  public Map<String, JpaTabAttrBaseDO<M, Integer>> getAttrs()
  {
    return attributes;
  }

  public void setAttrs(final Map<String, JpaTabAttrBaseDO<M, Integer>> attributes)
  {
    this.attributes = attributes;
  }

  @Transient
  public StringConverter getStringConverter()
  {
    return stringConverter;
  }

  public void setStringConverter(final StringConverter stringConverter)
  {
    this.stringConverter = stringConverter;
  }

  @Transient
  private M getThis()
  {
    return (M) this;
  }
}
