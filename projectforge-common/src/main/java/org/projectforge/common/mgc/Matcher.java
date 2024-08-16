//
// Copyright (C) 2010-2016 Micromata GmbH
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.projectforge.common.mgc;

import java.io.Serializable;
import java.util.Collection;

/**
 * Base class for matching.
 *
 * @author roger
 * @param <T> the generic type
 */
public interface Matcher<T>extends Serializable
{

  /**
   * Allgemeine Methode um ein Object auf das "Passen" zu überpüfen.
   *
   * @param object Das zu checkende Objekt
   * @return true, falls das Objekt passt
   */
  public boolean match(T object);

  /**
   * similar to match, but return 3 state.
   *
   * @param object the object
   * @return the match result
   */
  public MatchResult apply(T object);

  /**
   * Match any.
   *
   * @param sl the sl
   * @param defaultValue if none are matched, returns defualtValue
   * @return true, if successful
   */
  boolean matchAny(Collection<T> sl, boolean defaultValue);

  /**
   * Match all.
   *
   * @param sl the sl
   * @param defaultValue the default value
   * @return true, if successful
   */
  boolean matchAll(Collection<T> sl, boolean defaultValue);
}
