package org.projectforge.config;

import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.spring.SpringWebApplicationFactory;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.rest.config.CORSFilter;
import org.projectforge.security.SecurityHeaderFilter;
import org.projectforge.web.debug.SessionSerializableChecker;
import org.projectforge.web.doc.TutorialFilter;
import org.projectforge.web.filter.ResponseHeaderFilter;
import org.projectforge.web.filter.SpringThreadLocalFilter;
import org.projectforge.web.rest.RestUserFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.support.OpenSessionInViewFilter;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * This class is the replacement of the web.xml. It registers the wicket filter in the spring aware configuration style.
 *
 * @author Florian Blumenstein
 */
@Configuration
public class WebXMLInitializer implements ServletContextInitializer {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebXMLInitializer.class);

  @Value("${projectforge.security.csp-header-value:}") // defaults to empty string
  private String cspHeaderValue;

  @Value("${projectforge.web.development.enableCORSFilter:false}")
  private boolean webDevelopmentEnableCORSFilter;

  private static final String PARAM_APP_BEAN = "applicationBean";

  @Override
  public void onStartup(ServletContext sc) throws ServletException {
    final FilterRegistration securityHeaderFilter = sc.addFilter("SecurityHeaderFilter", SecurityHeaderFilter.class);
    securityHeaderFilter.addMappingForUrlPatterns(null, false, "/*");
    securityHeaderFilter.setInitParameter(SecurityHeaderFilter.PARAM_CSP_HEADER_VALUE, cspHeaderValue);

    final FilterRegistration userFilter = sc.addFilter("UserFilter", UserFilter.class);
    boolean filterAfterInternal = false;
    userFilter.addMappingForUrlPatterns(null, filterAfterInternal, "/secure/*");
    userFilter.addMappingForUrlPatterns(null, filterAfterInternal, "/wa/*");

    final FilterRegistration hibernateFilter = sc.addFilter("HibernateFilter", OpenSessionInViewFilter.class);
    hibernateFilter.setInitParameter("sessionFactoryBeanName", "sessionFactory");
    hibernateFilter.setInitParameter("singleSession", "false");
    hibernateFilter.addMappingForUrlPatterns(null, filterAfterInternal, "/wa/*");

    final FilterRegistration springContext = sc.addFilter("springContext", SpringThreadLocalFilter.class);
    springContext.addMappingForUrlPatterns(null, filterAfterInternal, "/secure/*");
    springContext.addMappingForUrlPatterns(null, filterAfterInternal, "/wa/*");

    final FilterRegistration wicketApp = sc.addFilter("wicket.app", WicketFilter.class);
    wicketApp.setInitParameter(WicketFilter.APP_FACT_PARAM, SpringWebApplicationFactory.class.getName());
    wicketApp.setInitParameter(PARAM_APP_BEAN, "wicketApplication");
    wicketApp.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/wa/*");
    wicketApp.addMappingForUrlPatterns(null, filterAfterInternal, "/wa/*");

    if (webDevelopmentEnableCORSFilter) {
      log.warn("*********************************");
      log.warn("***********            **********");
      log.warn("*********** ATTENTION! **********");
      log.warn("***********            **********");
      log.warn("*********** Running in **********");
      log.warn("*********** dev mode!  **********");
      log.warn("***********            **********");
      log.warn("*********************************");
      log.warn("Don't deliver this app in dev mode due to security reasons (cross origin allowed)!");
      sc.addFilter("cors", new CORSFilter()).addMappingForUrlPatterns(null, false,
              "/" + RestPaths.REST + "/*",
              "/" + RestPaths.PUBLIC_REST + "/*");
    }

    final FilterRegistration restUserFilter = sc.addFilter("restUserFilter", RestUserFilter.class);
    restUserFilter.addMappingForUrlPatterns(null, false, "/rest/*");


    final FilterRegistration expire = sc.addFilter("expire", ResponseHeaderFilter.class);
    expire.setInitParameter("Cache-Control", "public, max-age=7200");
    expire.addMappingForUrlPatterns(null, false, "*.css");
    expire.addMappingForUrlPatterns(null, false, "*.gif");
    expire.addMappingForUrlPatterns(null, false, "*.gspt");
    expire.addMappingForUrlPatterns(null, false, "*.jpg");
    expire.addMappingForUrlPatterns(null, false, "*.js");
    expire.addMappingForUrlPatterns(null, false, "*.png");
    expire.addMappingForUrlPatterns(null, false, "*.swf");

    final FilterRegistration tutorialFilter = sc.addFilter("TutorialFilter", TutorialFilter.class);
    tutorialFilter.addMappingForUrlPatterns(null, false, "/secure/doc/*");

    sc.addListener(SessionSerializableChecker.class);
  }

}
