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

/**
 * Matches always.
 *
 * @author roger
 * @param <T> the generic type
 */
public class EveryMatcher<T>extends MatcherBase<T>
{

  /**
   * The Constant serialVersionUID.
   */
  private static final long serialVersionUID = -6603198094053117381L;

  /**
   * The singleton.
   */
  private static EveryMatcher<?> SINGLETON = new EveryMatcher<Object>();

  /**
   * Single instanceof.
   *
   * @param <T> the generic type
   * @return the every matcher
   */
  @SuppressWarnings("unchecked")
  public static <T> EveryMatcher<T> everyMatcher()
  {
    return (EveryMatcher<T>) SINGLETON;
  }

  @Override
  public boolean match(T token)
  {
    return true;
  }

  @Override
  public String toString()
  {
    return "*";
  }

}
