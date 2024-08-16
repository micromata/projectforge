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

import java.util.Collection;

/**
 * Base class of Matcher.
 *
 * @author roger
 * @param <T> the generic type
 */
public abstract class MatcherBase<T>implements Matcher<T>
{

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = 7157263544470217750L;

  @Override
  public MatchResult apply(T token)
  {
    return match(token) ? MatchResult.MatchPositive : MatchResult.NoMatch;
  }

  @Override
  public boolean matchAll(Collection<T> sl, boolean defaultValue)
  {
    boolean matches = defaultValue;
    for (T token : sl) {
      MatchResult mr = apply(token);
      if (mr == MatchResult.NoMatch) {
        return false;
      }
      if (mr == MatchResult.MatchPositive) {
        matches = true;
      }
    }
    return matches;
  }

  @Override
  public boolean matchAny(Collection<T> sl, boolean defaultValue)
  {
    boolean matches = defaultValue;
    for (T token : sl) {
      MatchResult mr = apply(token);
      if (mr == MatchResult.MatchPositive) {
        return true;
      }
    }
    return matches;
  }
}
