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

package org.projectforge.common.mgc;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Similar to c++ type pair.
 *
 * @author roger@micromata.de
 * @param <K> the key type
 * @param <V> the value type
 */
public class Pair<K, V> implements Map.Entry<K, V>, Serializable
{

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = 1427196812388547552L;

  /**
   * The key.
   */
  private K key;

  /**
   * The value.
   */
  private V value;

  /**
   * Make.
   *
   * @param <MK> the generic type
   * @param <MV> the generic type
   * @param key the key
   * @param value the value
   * @return the pair
   */
  public static <MK, MV> Pair<MK, MV> make(MK key, MV value)
  {
    return new Pair<MK, MV>(key, value);
  }

  /**
   * Builds null-save clone.
   *
   * @param <MK> the generic type
   * @param <MV> the generic type
   * @param source the source
   * @return the pair
   */
  public static <MK, MV> Pair<MK, MV> make(Pair<MK, MV> source)
  {
    if (source == null) {
      return null;
    }
    return new Pair<MK, MV>(source.getKey(), source.getValue());
  }

  /**
   * Instantiates a new pair.
   */
  public Pair()
  {
  }

  /**
   * Instantiates a new pair.
   *
   * @param key the key
   * @param value the value
   */
  public Pair(K key, V value)
  {
    this.key = key;
    this.value = value;
  }

  @Override
  public K getKey()
  {
    return key;
  }

  public void setKey(K key)
  {
    this.key = key;
  }

  @Override
  public V getValue()
  {
    return value;
  }

  @Override
  public V setValue(V value)
  {
    V t = this.value;
    this.value = value;
    return t;
  }

  public K getFirst()
  {
    return key;
  }

  public V getSecond()
  {
    return value;
  }

  public void setFirst(K k)
  {
    key = k;
  }

  public void setSecond(V v)
  {
    value = v;
  }

  @Override
  public String toString()
  {
    return key + ": " + value;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj)
  {
    if (obj instanceof Pair) {
      Pair other = (Pair) obj;
      return Objects.equals(key, other.key) && Objects.equals(value, other.value);

    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return Objects.hashCode(key) * 31 + Objects.hashCode(value);
  }
}
