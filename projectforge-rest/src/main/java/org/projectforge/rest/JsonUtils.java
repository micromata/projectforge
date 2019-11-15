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

package org.projectforge.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Serialization and deserialization for rest calls.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class JsonUtils {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonUtils.class);

  private static Map<Class<?>, Object> typeAdapterMap = new HashMap<>();

  private static ObjectMapper objectMapper;

  public static void add(final Class<?> cls, final Object typeAdapter) {
    typeAdapterMap.put(cls, typeAdapter);
  }

  public static String toJson(final Object obj) {
    try {
      return getObjectMapper().writeValueAsString(obj);
    } catch (JsonProcessingException ex) {
      log.error(ex.getMessage(), ex);
      return "";
    }
  }

  public static <T> T fromJson(final String json, final Class<T> classOfT) throws IOException {
    return getObjectMapper().readValue(json, classOfT);
  }

  private static ObjectMapper getObjectMapper() {
    if (objectMapper == null) {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new KotlinModule());
      objectMapper = mapper;
    }
    return objectMapper;
  }
}
