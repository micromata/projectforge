package org.projectforge.launcher;

import javax.servlet.FilterRegistration;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.wicket.protocol.http.WicketFilter;
import org.apache.wicket.spring.SpringWebApplicationFactory;
import org.projectforge.business.user.filter.UserFilter;
import org.projectforge.web.debug.SessionSerializableChecker;
import org.projectforge.web.doc.TutorialFilter;
import org.projectforge.web.filter.ResponseHeaderFilter;
import org.projectforge.web.filter.SpringThreadLocalFilter;
import org.projectforge.web.rest.RestUserFilter;
import org.springframework.boot.autoconfigure.web.MultipartProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.support.OpenSessionInViewFilter;

import de.micromata.genome.util.runtime.LocalSettings;
import de.micromata.mgc.springbootapp.ApplicationServletInitializer;

/**
 * This class is the replacement of the web.xml. It registers the wicket filter in the spring aware configuration style.
 *
 * @author Florian Blumenstein
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Configuration
public class WebXMLInitializer extends ApplicationServletInitializer
{

  private static final String PARAM_APP_BEAN = "applicationBean";

  @Override
  public void onStartup(ServletContext sc) throws ServletException
  {
    FilterRegistration userFilter = sc.addFilter("UserFilter",
        UserFilter.class);
    boolean filterAfterInternal = false;
    userFilter.addMappingForUrlPatterns(null, filterAfterInternal, "/secure/*");
    userFilter.addMappingForUrlPatterns(null, filterAfterInternal, "/wa/*");

    FilterRegistration hibernateFilter = sc.addFilter("HibernateFilter",
        OpenSessionInViewFilter.class);
    hibernateFilter.setInitParameter("sessionFactoryBeanName",
        "sessionFactory");
    hibernateFilter.setInitParameter("singleSession",
        "false");
    hibernateFilter.addMappingForUrlPatterns(null, filterAfterInternal, "/wa/*");

    FilterRegistration springContext = sc.addFilter("springContext",
        SpringThreadLocalFilter.class);
    springContext.addMappingForUrlPatterns(null, filterAfterInternal, "/secure/*");
    springContext.addMappingForUrlPatterns(null, filterAfterInternal, "/wa/*");

    FilterRegistration wicketApp = sc.addFilter("wicket.app",
        WicketFilter.class);
    wicketApp.setInitParameter(WicketFilter.APP_FACT_PARAM,
        SpringWebApplicationFactory.class.getName());
    wicketApp.setInitParameter(PARAM_APP_BEAN, "wicketApplication");
    wicketApp.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/wa/*");
    wicketApp.addMappingForUrlPatterns(null, filterAfterInternal, "/wa/*");

    FilterRegistration restUserFilter = sc.addFilter("restUserFilter",
        RestUserFilter.class);
    restUserFilter.addMappingForUrlPatterns(null, false, "/rest/*");

    FilterRegistration expire = sc.addFilter("expire",
        ResponseHeaderFilter.class);
    expire.setInitParameter("Cache-Control",
        "public, max-age=7200");
    expire.addMappingForUrlPatterns(null, false, "*.css");
    expire.addMappingForUrlPatterns(null, false, "*.gif");
    expire.addMappingForUrlPatterns(null, false, "*.gspt");
    expire.addMappingForUrlPatterns(null, false, "*.jpg");
    expire.addMappingForUrlPatterns(null, false, "*.js");
    expire.addMappingForUrlPatterns(null, false, "*.png");
    expire.addMappingForUrlPatterns(null, false, "*.swf");

    FilterRegistration tutorialFilter = sc.addFilter("TutorialFilter",
        TutorialFilter.class);
    tutorialFilter.addMappingForUrlPatterns(null, false, "/secure/doc/*");

    sc.addListener(SessionSerializableChecker.class);

    //    Dynamic restApplication = sc.addServlet("RestApplication", SpringServlet.class);
    //    restApplication.setInitParameter("javax.ws.rs.Application", "org.projectforge.web.rest.RestApplication");
    //    restApplication.setLoadOnStartup(1);
    //    restApplication.addMapping("/rest/*");
    //    int maxSize = LocalSettings.get().getIntValue("pf.config.maxUploadSize", 0);
    //    if (maxSize != 0) {
    //      System.getProperties().setProperty("org.eclipse.jetty.server.Request.maxFormContentSize",
    //          Integer.toString(maxSize));
    //    }
    //sc.getSessionCookieConfig().setHttpOnly(true);

  }

  @Bean
  public MultipartConfigElement multipartConfigElement()
  {
    MultipartProperties mpp = new MultipartProperties();
    LocalSettings ls = LocalSettings.get();
    mpp.setMaxFileSize(ls.get("multipart.maxFileSize"));
    mpp.setLocation(ls.get("multipart.location"));
    mpp.setMaxRequestSize(ls.get("multipart.maxRequestSize"));
    mpp.setFileSizeThreshold(ls.get("fileSizeThreshold"));
    return mpp.createMultipartConfig();
  }

}
