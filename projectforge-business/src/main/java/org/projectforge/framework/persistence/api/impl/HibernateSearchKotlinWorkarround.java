/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.api.impl;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.projectforge.framework.persistence.api.ExtendedBaseDO;

class HibernateSearchKotlinWorkarround {
    /**
     * Workarround for Kotlin: LuceneExtension.get() doesn't work in Kotlin, or I don't know how to fix the compiler errors on missing type variables.
     */
    @SuppressWarnings("rawtypes")
    static SearchQueryOptionsStep luceneExtension(final SearchSession searchSession, final Class<? extends ExtendedBaseDO<Long>> doClass, final Query luceneQuery) {
        return searchSession.search(doClass)
                .extension(LuceneExtension.get())
                .where(lucene -> lucene.fromLuceneQuery(luceneQuery)); // Lucene-Abfrage
    }
}
