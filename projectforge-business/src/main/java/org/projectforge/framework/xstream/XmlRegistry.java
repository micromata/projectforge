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

package org.projectforge.framework.xstream;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.projectforge.Version;
import org.projectforge.framework.xstream.converter.BigDecimalConverter;
import org.projectforge.framework.xstream.converter.BooleanConverter;
import org.projectforge.framework.xstream.converter.ByteArrayConverter;
import org.projectforge.framework.xstream.converter.ClassConverter;
import org.projectforge.framework.xstream.converter.DateConverter;
import org.projectforge.framework.xstream.converter.DoubleConverter;
import org.projectforge.framework.xstream.converter.IConverter;
import org.projectforge.framework.xstream.converter.IntConverter;
import org.projectforge.framework.xstream.converter.LocaleConverter;
import org.projectforge.framework.xstream.converter.LongConverter;
import org.projectforge.framework.xstream.converter.ShortConverter;
import org.projectforge.framework.xstream.converter.StringConverter;
import org.projectforge.framework.xstream.converter.TimeZoneConverter;
import org.projectforge.framework.xstream.converter.VersionConverter;

/**
 * There is one singleton instance used for default mappings, aliases etc.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class XmlRegistry
{
  private static final XmlRegistry baseRegistry;

  private Map<Class< ? >, IConverter< ? >> converterRegistry;

  // private Map<Field, IConverter< ? >> fieldConverterRegistry;

  private Set<Class< ? >> asAttributeAsDefaultSet;

  private AliasMap aliasMap;

  static {
    baseRegistry = new XmlRegistry();
    baseRegistry.initializeDefaultRegistry();
  }

  /**
   * Default xml registry.
   */
  public static XmlRegistry baseRegistry()
  {
    return baseRegistry;
  }

  /**
   * For customized xml registry.
   */
  public XmlRegistry()
  {
  }

  /**
   * Only for the singleton instance.
   */
  private void initializeDefaultRegistry()
  {
    internalRegisterTypeAsAttribute(BigDecimal.class);
    internalRegisterTypeAsAttribute(Boolean.class);
    internalRegisterTypeAsAttribute(boolean.class);
    internalRegisterTypeAsAttribute(Date.class);
    internalRegisterTypeAsAttribute(Double.class);
    internalRegisterTypeAsAttribute(double.class);
    internalRegisterTypeAsAttribute(Integer.class);
    internalRegisterTypeAsAttribute(int.class);
    internalRegisterTypeAsAttribute(Long.class);
    internalRegisterTypeAsAttribute(long.class);
    internalRegisterTypeAsAttribute(Short.class);
    internalRegisterTypeAsAttribute(short.class);
    internalRegisterTypeAsAttribute(TimeZone.class);
    internalRegisterTypeAsAttribute(Locale.class);
    internalRegisterTypeAsAttribute(Version.class);

    internalRegisterAlias(BigDecimal.class, "decimal");
    internalRegisterAlias(Boolean.class, "boolean");
    internalRegisterAlias(boolean.class, "boolean");
    internalRegisterAlias(Double.class, "double");
    internalRegisterAlias(double.class, "double");
    internalRegisterAlias(Integer.class, "int");
    internalRegisterAlias(int.class, "int");
    internalRegisterAlias(Long.class, "long");
    internalRegisterAlias(long.class, "long");
    internalRegisterAlias(Short.class, "short");
    internalRegisterAlias(short.class, "short");
    internalRegisterAlias(TimeZone.class, "timeZone");
    internalRegisterAlias(Locale.class, "locale");
    internalRegisterAlias(Version.class, "version");

    IConverter< ? > conv = new BooleanConverter();
    internalRegisterConverter(Boolean.class, conv);
    internalRegisterConverter(boolean.class, conv);
    conv = new DoubleConverter();
    internalRegisterConverter(Double.class, conv);
    internalRegisterConverter(double.class, conv);
    conv = new IntConverter();
    internalRegisterConverter(Integer.class, conv);
    internalRegisterConverter(int.class, conv);
    conv = new LongConverter();
    internalRegisterConverter(Long.class, conv);
    internalRegisterConverter(long.class, conv);
    conv = new ShortConverter();
    internalRegisterConverter(Short.class, conv);
    internalRegisterConverter(short.class, conv);
    internalRegisterConverter(byte[].class, new ByteArrayConverter());

    internalRegisterConverter(BigDecimal.class, new BigDecimalConverter());
    internalRegisterConverter(Class.class, new ClassConverter());
    internalRegisterConverter(Date.class, new DateConverter());
    internalRegisterConverter(String.class, new StringConverter());
    internalRegisterConverter(TimeZone.class, new TimeZoneConverter());
    internalRegisterConverter(Locale.class, new LocaleConverter());
    internalRegisterConverter(Version.class, new VersionConverter());
  }

  public IConverter< ? > getConverter(final Class< ? > clazz)
  {
    if (converterRegistry != null) {
      final IConverter< ? > converter = converterRegistry.get(clazz);
      if (converter != null) {
        return converter;
      }
      for (final Class< ? > cls : converterRegistry.keySet()) {
        if (cls.isAssignableFrom(clazz) == true) {
          final IConverter< ? > conv = converterRegistry.get(cls);
          if (conv != null) {
            // Register so every type must only be checked once.
            internalRegisterConverter(clazz, conv);
            return conv;
          }
        }
      }
    }
    if (this == baseRegistry) {
      return null;
    }
    return baseRegistry.getConverter(clazz);
  }

  public boolean asAttributeAsDefault(final Class< ? > type)
  {
    if (asAttributeAsDefaultSet != null) {
      if (asAttributeAsDefaultSet.contains(type) == true) {
        return true;
      }
    }
    if (this == baseRegistry) {
      return false;
    }
    return baseRegistry.asAttributeAsDefault(type);
  }

  public String getAliasForClass(final Class< ? > type)
  {
    if (aliasMap != null) {
      final String alias = aliasMap.getAliasForClass(type);
      if (alias != null) {
        return alias;
      }
    }
    if (this == baseRegistry) {
      return null;
    }
    return baseRegistry.getAliasForClass(type);
  }

  public Class< ? > getClassForAlias(final String alias)
  {
    if (aliasMap != null) {
      final Class< ? > clazz = aliasMap.getClassForAlias(alias);
      if (clazz != null) {
        return clazz;
      }
    }
    if (this == baseRegistry) {
      return null;
    }
    return baseRegistry.getClassForAlias(alias);
  }

  public XmlRegistry registerConverter(final Class< ? > type, final IConverter< ? > converter)
  {
    if (this == baseRegistry) {
      throw new IllegalStateException("Couldn't register type as attribute for base registry (singleton). This registry is unmodifiable");
    }
    internalRegisterConverter(type, converter);
    return this;
  }

  private void internalRegisterConverter(final Class< ? > type, final IConverter< ? > converter)
  {
    if (this.converterRegistry == null) {
      this.converterRegistry = new HashMap<Class< ? >, IConverter< ? >>();
    }
    this.converterRegistry.put(type, converter);
  }

  public XmlRegistry registerAlias(final Class< ? > type, final String alias)
  {
    if (this == baseRegistry) {
      throw new IllegalStateException("Couldn't register type as attribute for base registry (singleton). This registry is unmodifiable");
    }
    internalRegisterAlias(type, alias);
    return this;
  }

  private void internalRegisterAlias(final Class< ? > type, final String alias)
  {
    if (this.aliasMap == null) {
      this.aliasMap = new AliasMap();
    }
    this.aliasMap.put(type, alias);
  }

  public XmlRegistry registerTypeAsAttribute(final Class< ? > type)
  {
    if (this == baseRegistry) {
      throw new IllegalStateException("Couldn't register type as attribute for base registry (singleton). This registry is unmodifiable");
    }
    internalRegisterTypeAsAttribute(type);
    return this;
  }

  private void internalRegisterTypeAsAttribute(final Class< ? > type)
  {
    if (this.asAttributeAsDefaultSet == null) {
      this.asAttributeAsDefaultSet = new HashSet<Class< ? >>();
    }
    this.asAttributeAsDefaultSet.add(type);
  }
}
