package org.projectforge.framework.persistence.jpa.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Id;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.ClassicAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.annotations.DocumentId;
import org.projectforge.common.BeanHelper;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.time.DateFormats;
import org.projectforge.framework.time.DateHelper;

import de.micromata.genome.db.jpa.history.api.WithHistory;
import de.micromata.genome.util.runtime.ClassUtils;

/**
 * Some methods pulled out of BaseDao.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class HibernateSearchFilterUtils
{
  private static final Logger LOG = Logger.getLogger(HibernateSearchFilterUtils.class);
  private static final String[] HISTORY_SEARCH_FIELDS = { "NEW_VALUE", "OLD_VALUE" };
  private static final String[] luceneReservedWords = { "AND", "OR", "NOT" };

  /**
   * Additional allowed characters (not at first position) for search string modification with wildcards. Do not forget
   * to update I18nResources.properties and the user documentation after any changes. <br/>
   * ALLOWED_CHARS = @._-+*
   */
  private static final String ALLOWED_CHARS = "@._-+*";

  /**
   * Additional allowed characters (at first position) for search string modification with wildcards. Do not forget to
   * update I18nResources.properties and the user documentation after any changes. <br/>
   * ALLOWED_BEGINNING_CHARS =
   * 
   * @._
   */
  private static final String ALLOWED_BEGINNING_CHARS = "@._*";
  /**
   * If the search string containts any of this escape chars, no string modification will be done.
   */
  private static final String ESCAPE_CHARS = "+-";

  /**
   * If the search string starts with "'" then the searchString will be returned without leading "'". If the search
   * string consists only of alphanumeric characters and allowed chars and spaces the wild card character '*' will be
   * appended for enable ...* search. Otherwise the searchString itself will be returned.
   * 
   * @param searchString
   * @param andSearch If true then all terms must match (AND search), otherwise OR will used (default)
   * @see #ALLOWED_CHARS
   * @see #ALLOWED_BEGINNING_CHARS
   * @see #ESCAPE_CHARS
   * @return The modified search string or the original one if no modification was done.
   */
  public static String modifySearchString(final String searchString, final boolean andSearch)
  {
    if (searchString == null) {
      return "";
    }
    if (searchString.startsWith("'") == true) {
      return searchString.substring(1);
    }
    for (int i = 0; i < searchString.length(); i++) {
      final char ch = searchString.charAt(i);
      if (Character.isLetterOrDigit(ch) == false && Character.isWhitespace(ch) == false) {
        final String allowed = (i == 0) ? ALLOWED_BEGINNING_CHARS : ALLOWED_CHARS;
        if (allowed.indexOf(ch) < 0) {
          return searchString;
        }
      }
    }
    final String[] tokens = StringUtils.split(searchString, ' ');
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final String token : tokens) {
      if (first == true) {
        first = false;
      } else {
        buf.append(" ");
      }
      if (ArrayUtils.contains(luceneReservedWords, token) == false) {
        final String modified = modifySearchToken(token);
        if (tokens.length > 1 && andSearch == true && StringUtils.containsNone(modified, ESCAPE_CHARS) == true) {
          buf.append("+");
        }
        buf.append(modified);
        if (modified.endsWith("*") == false && StringUtils.containsNone(modified, ESCAPE_CHARS) == true) {
          if (andSearch == false || tokens.length > 1) {
            // Don't append '*' if used by SearchForm and only one token is given. It's will be appended automatically by BaseDao before the
            // search is executed.
            buf.append('*');
          }
        }
      } else {
        buf.append(token);
      }
    }
    return buf.toString();
  }

  /**
   * @see #modifySearchString(String, boolean)
   */
  public static String modifySearchString(final String searchString)
  {
    return modifySearchString(searchString, false);
  }

  /**
   * Does nothing (because it seems to be work better in most times). Quotes special Lucene characters: '-' -> "\-"
   * 
   * @param searchToken One word / token of the search string (one entry of StringUtils.split(searchString, ' ')).
   * @return
   */
  protected static String modifySearchToken(final String searchToken)
  {
    final StringBuffer buf = new StringBuffer();
    for (int i = 0; i < searchToken.length(); i++) {
      final char ch = searchToken.charAt(i);
      /*
       * if (ESCAPE_CHARS.indexOf(ch) >= 0) { buf.append('\\'); }
       */
      buf.append(ch);
    }
    return buf.toString();
  }

  private static Set<Class<?>> getNestedHistoryEntities(Class<?> clazz)
  {
    List<WithHistory> whs = ClassUtils.findClassAnnotations(clazz, WithHistory.class);
    Set<Class<?>> nested = new HashSet<>();
    for (WithHistory wh : whs) {
      for (Class<?> cls : wh.nestedEntities()) {
        nested.add(cls);
      }
    }
    return nested;
  }

  public static void getHistoryEntriesFromSearch(Session session, BaseSearchFilter filter,
      Set<Integer> idSet,
      Class<?> clazz)
  {
    String className = clazz.getName();//ClassUtils.getShortClassName(clazz);

    final StringBuffer buf = new StringBuffer();
    buf.append("(+entityName:").append(className);
    if (filter.getStartTimeOfModification() != null || filter.getStopTimeOfModification() != null) {
      final DateFormat df = new SimpleDateFormat(DateFormats.LUCENE_TIMESTAMP_MINUTE);
      df.setTimeZone(DateHelper.UTC);
      buf.append(" +modifiedAt:[");
      if (filter.getStartTimeOfModification() != null) {
        buf.append(df.format(filter.getStartTimeOfModification()));
      } else {
        buf.append("000000000000");
      }
      buf.append(" TO ");
      if (filter.getStopTimeOfModification() != null) {
        buf.append(df.format(filter.getStopTimeOfModification()));
      } else {
        buf.append("999999999999");
      }
      buf.append("]");
    }
    if (filter.getModifiedByUserId() != null) {
      buf.append(" +modifiedBy:").append(filter.getModifiedByUserId());
    }
    buf.append(") AND (");
    final String searchString = buf.toString()
        + HibernateSearchFilterUtils.modifySearchString(filter.getSearchString()) + ")";
    try {
      final FullTextSession fullTextSession = Search.getFullTextSession(session);

      final org.apache.lucene.search.Query query = createFullTextQuery(fullTextSession, HISTORY_SEARCH_FIELDS, null,
          searchString, PfHistoryMasterDO.class);
      if (query == null) {
        // An error occured:
        return;
      }
      final FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(query, PfHistoryMasterDO.class);
      fullTextQuery.setCacheable(true);
      fullTextQuery.setCacheRegion("historyItemCache");
      fullTextQuery.setProjection("entityId");
      final List<Object[]> result = fullTextQuery.list();
      if (result != null && result.size() > 0) {
        for (final Object[] oa : result) {
          idSet.add((Integer) oa[0]);
        }
      }
    } catch (final Exception ex) {
      final String errorMsg = "Lucene error message: "
          + ex.getMessage()
          + " (for "
          + clazz.getSimpleName()
          + ": "
          + searchString
          + ").";
      filter.setErrorMessage(errorMsg);
      LOG.error(errorMsg);
    }
  }

  public static void getHistoryEntriesDirect(Session session, BaseSearchFilter filter,
      Set<Integer> idSet,
      Class<?> clazz)
  {
    Criteria criteria = session.createCriteria(PfHistoryMasterDO.class);

    //      setCacheRegion(criteria);
    Object className = clazz.getName();
    criteria.add(Restrictions.eq("entityName", className));
    if (filter.getStartTimeOfModification() != null && filter.getStopTimeOfModification() != null) {
      criteria.add(
          Restrictions.between("modifiedAt", filter.getStartTimeOfModification(), filter.getStopTimeOfModification()));
    } else if (filter.getStartTimeOfModification() != null) {
      criteria.add(Restrictions.ge("modifiedAt", filter.getStartTimeOfModification()));
    } else if (filter.getStopTimeOfModification() != null) {
      criteria.add(Restrictions.le("modifiedAt", filter.getStopTimeOfModification()));
    }
    if (filter.getModifiedByUserId() != null) {
      criteria.add(Restrictions.eq("modifiedBy", filter.getModifiedByUserId().toString()));
    }
    criteria.setCacheable(true);
    criteria.setCacheRegion("historyItemCache");
    criteria.setProjection(Projections.property("entityId"));
    List<Object> idList = criteria.list();
    if (idList != null && idList.size() > 0) {
      for (Object id : idList) {
        if (id instanceof Number) {
          idSet.add(((Number) id).intValue());
        } else {
          LOG.warn("ID is not a number: " + id);
        }

      }
    }
    for (Class<?> nested : getNestedHistoryEntities(clazz)) {
      getHistoryEntriesDirect(session, filter, idSet, nested);
    }
  }

  private static Set<String> getSearchFields(Class<?> clazz, String[] additionalSearchFields)
  {
    PfEmgrFactory emf = ApplicationContextProvider.getApplicationContext().getBean(PfEmgrFactory.class);
    Set<String> ret = emf.getSearchableTextFieldsForEntity(clazz);

    if (additionalSearchFields == null) {
      return ret;
    }
    for (String addf : additionalSearchFields) {
      if (ret.contains(addf) == false) {
        LOG.warn("Searchfield added: " + clazz.getName() + "." + addf);
        ret.add(addf);
      }
    }
    return ret;
  }

  public static String[] determineSearchFields(Class<?> clazz)
  {
    return determineSearchFields(clazz, new String[] {});
  }

  public static String[] determineSearchFields(Class<?> clazz, String[] additionalSearchFields)
  {
    boolean useMeta = true;
    if (useMeta == true) {
      Set<String> set = getSearchFields(clazz, additionalSearchFields);
      return set.toArray(new String[] {});
    }

    final Field[] fields = BeanHelper.getAllDeclaredFields(clazz);
    final Set<String> fieldNames = new TreeSet<String>();
    for (final Field field : fields) {
      if (field.isAnnotationPresent(org.hibernate.search.annotations.Field.class) == true) {
        // @Field(index = Index.YES /*TOKENIZED*/),
        final org.hibernate.search.annotations.Field annotation = field
            .getAnnotation(org.hibernate.search.annotations.Field.class);
        fieldNames.add(getSearchName(field.getName(), annotation));
      } else if (field.isAnnotationPresent(org.hibernate.search.annotations.Fields.class) == true) {
        // @Fields( {
        // @Field(index = Index.YES /*TOKENIZED*/),
        // @Field(name = "name_forsort", index = Index.YES, analyze = Analyze.NO /*UN_TOKENIZED*/)
        // } )
        final org.hibernate.search.annotations.Fields annFields = field
            .getAnnotation(org.hibernate.search.annotations.Fields.class);
        for (final org.hibernate.search.annotations.Field annotation : annFields.value()) {
          fieldNames.add(getSearchName(field.getName(), annotation));
        }
      } else if (field.isAnnotationPresent(Id.class) == true) {
        fieldNames.add(field.getName());
      } else if (field.isAnnotationPresent(DocumentId.class) == true) {
        fieldNames.add(field.getName());
      }
    }
    final Method[] methods = clazz.getMethods();
    for (final Method method : methods) {
      if (method.isAnnotationPresent(org.hibernate.search.annotations.Field.class) == true) {
        final org.hibernate.search.annotations.Field annotation = method
            .getAnnotation(org.hibernate.search.annotations.Field.class);
        fieldNames.add(getSearchName(method.getName(), annotation));
      } else if (method.isAnnotationPresent(DocumentId.class) == true) {
        final String prop = BeanHelper.determinePropertyName(method);
        fieldNames.add(prop);
      }
    }
    if (additionalSearchFields != null) {
      for (final String str : additionalSearchFields) {
        fieldNames.add(str);
      }
    }
    String[] searchFields = fieldNames.toArray(new String[] {});
    LOG.info("Search fields for '" + clazz + "': " + ArrayUtils.toString(searchFields));
    return searchFields;
  }

  private static String getSearchName(final String fieldName, final org.hibernate.search.annotations.Field annotation)
  {
    if (StringUtils.isNotEmpty(annotation.name()) == true) {
      // Name of field is changed for hibernate-search via annotation:
      return annotation.name();
    } else {
      return fieldName;
    }
  }

  public static org.apache.lucene.search.Query createFullTextQuery(FullTextSession fullTextSession,
      String[] searchFields,
      QueryFilter filter, String searchString, Class<?> clazz)
  {
    final MultiFieldQueryParser parser = new MultiFieldQueryParser(searchFields, new ClassicAnalyzer());
    parser.setAllowLeadingWildcard(true);
    org.apache.lucene.search.Query query = null;
    try {
      query = parser.parse(searchString);
    } catch (final org.apache.lucene.queryparser.classic.ParseException ex) {
      final String errorMsg = "Lucene error message: "
          + ex.getMessage()
          + " (for "
          + clazz.getSimpleName()
          + ": "
          + searchString
          + ").";
      // TODO feedback
      LOG.error(errorMsg);
      return null;
    }
    return query;
    //    BooleanQuery.Builder bb =  new BooleanQuery.Builder();
    //    for (String field : searchFields) {
    //      QueryParser qp = new QueryParser(field, SearchEngine.ANALYZER);
    //      fieldsQuery.add(qp.parse(string), BooleanClause.Occur.SHOULD);
    //    }
    //
    //    QueryContextBuilder builder = fullTextSession.getSearchFactory().buildQueryBuilder();
    //    final org.apache.lucene.search.Query query = builder.forEntity(clazz)
    //        .get().keyword().wildcard().onFields(searchFields)
    //
    //        .matching(searchString).createQuery();
    //    return query;
  }
}
