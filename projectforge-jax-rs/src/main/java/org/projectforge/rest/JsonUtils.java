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

package org.projectforge.rest;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.projectforge.rest.converter.UTCDateTimeTypeAdapter;
import org.projectforge.rest.converter.UTCDateTypeAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Serialization and deserialization for rest calls.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class JsonUtils
{
  private static Map<Class< ? >, Object> typeAdapterMap = new HashMap<Class< ? >, Object>();

  public static void add(final Class< ? > cls, final Object typeAdapter)
  {
    typeAdapterMap.put(cls, typeAdapter);
  }

  public static String toJson(final Object obj)
  {
    return createGson().toJson(obj);
  }

  public static <T> T fromJson(final String json, final Class<T> classOfT) throws JsonSyntaxException
  {
    return createGson().fromJson(json, classOfT);

  }

  @SuppressWarnings("unchecked")
  public static <T> T fromJson(final String json, final Type typeOfT) throws JsonSyntaxException
  {
    return (T) createGson().fromJson(json, typeOfT); // Cast (T) needed for Java 1.6.
  }

  private static Gson createGson()
  {
    final GsonBuilder builder = new GsonBuilder()//
    .registerTypeAdapter(java.sql.Date.class, new UTCDateTypeAdapter())//
    .registerTypeAdapter(java.util.Date.class, new UTCDateTimeTypeAdapter());
    for (final Map.Entry<Class< ? >, Object> entry : typeAdapterMap.entrySet()) {
      builder.registerTypeHierarchyAdapter(entry.getKey(), entry.getValue());
    }
    return builder.create();
  }
}
