/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.web;

import org.projectforge.SystemStatus;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.fibu.*;
import org.projectforge.business.fibu.datev.DatevImportService;
import org.projectforge.business.fibu.kost.BuchungssatzDao;
import org.projectforge.business.fibu.kost.Kost1Dao;
import org.projectforge.business.fibu.kost.Kost2Dao;
import org.projectforge.business.fibu.kost.KostCache;
import org.projectforge.business.sipgate.SipgateConfiguration;
import org.projectforge.business.task.TaskDao;
import org.projectforge.business.task.TaskTree;
import org.projectforge.business.teamcal.event.TeamEventDao;
import org.projectforge.business.timesheet.TimesheetDao;
import org.projectforge.business.user.*;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessDao;
import org.projectforge.framework.configuration.ConfigurationDao;
import org.projectforge.framework.persistence.database.DatabaseDao;
import org.projectforge.framework.persistence.database.DatabaseInitTestDataService;
import org.projectforge.framework.persistence.search.HibernateSearchReindexer;
import org.projectforge.jcr.JCRCheckSanityCheckJob;
import org.projectforge.menu.builder.MenuCreator;
import org.projectforge.plugins.core.PluginAdminService;
import org.projectforge.sms.SmsSenderConfig;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Need by Wicket during the migration phase to Kotlin/Rest, because Wicket/CG-LIB doesn't work properly with
 * SpringBean and Kotlin based Spring components.
 *
 * I didn't want to declare all Kotlin components as open, so I use this workaround. Wicket will be removed in the future.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */

public class WicketSupport {
    private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WicketSupport.class);

    private static WicketSupport instance = new WicketSupport();

    public static AccessChecker getAccessChecker() {
        return get(AccessChecker.class);
    }

    public static UserDao getUserDao() {
        return get(UserDao.class);
    }

    public static UserGroupCache getUserGroupCache() {
        return get(UserGroupCache.class);
    }

    /**
     * Workaround for SpringBean and Kotlin Spring components issues.
     *
     * @see WicketSupport
     */
    public static MenuCreator getMenuCreator() {
        return instance.getBean(MenuCreator.class);
    }

    /**
     * Workaround for SpringBean and Kotlin Spring components issues.
     *
     * @see WicketSupport
     */
    public static UserPrefCache getUserPrefCache() {
        return instance.getBean(UserPrefCache.class);
    }

    /**
     * Workaround for SpringBean and Kotlin Spring components issues.
     *
     * @see WicketSupport
     */
    public static SystemStatus getSystemStatus() {
        return instance.getBean(SystemStatus.class);
    }

    public static KostCache getKostCache() {
        return instance.getBean(KostCache.class);
    }

    public static TaskDao getTaskDao() {
        return instance.getBean(TaskDao.class);
    }

    public static AccessDao getAccessDao() {
        return instance.getBean(AccessDao.class);
    }

    public static <T> T get(Class<T> clazz) {
        return instance.getBean(clazz);
    }

    public static <T> void register(Class<T> clazz, T bean) {
        instance.registerBean(clazz, bean);
    }


    public static void register(ApplicationContext applicationContext) {
        WicketSupport.getInstance().registerBeans(applicationContext);
    }

    private Map<Class<?>, Object> componentsMap = new HashMap<>();

    private ApplicationContext applicationContext;

    private WicketSupport() {
    }

    private static WicketSupport getInstance() {
        return instance;
    }

    private void registerBean(Object component) {
        Class<?> clazz = component.getClass();
        registerBean(clazz, component);
    }

    private void registerBean(Class<?> clazz, Object component) {
        if (componentsMap.containsKey(clazz) && componentsMap.get(clazz) != component) {
            log.error("An object for the given clazz " + clazz.getName() + " is already registered and will be overwritten.");
        }
        componentsMap.put(clazz, component);
    }

    private <T> T getBean(Class<T> clazz) {
        T bean = (T) componentsMap.get(clazz);
        if (bean == null) {
            bean = applicationContext.getBean(clazz);
            registerBean(clazz, bean);
        }
        return bean;
    }


    private void registerBeans(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        // It doesn't matter if beans aren't registered. They will be registered on the fly.
        // Wicket workaround for not be able to proxy Kotlin base SpringBeans:
        registerBean(applicationContext.getBean(AccessDao.class));
        registerBean(applicationContext.getBean(AddressbookDao.class));
        // WicketSupport.getInstance().register(applicationContext.getBean(AddressCampaignValueDao.class));
        registerBean(applicationContext.getBean(AddressDao.class));
        registerBean(applicationContext.getBean(AuftragDao.class));
        registerBean(applicationContext.getBean(BuchungssatzDao.class));
        registerBean(applicationContext.getBean(ConfigurationDao.class));
        registerBean(applicationContext.getBean(ConfigurationService.class));
        registerBean(applicationContext.getBean(DatabaseDao.class));
        registerBean(applicationContext.getBean(DatabaseInitTestDataService.class));
        registerBean(applicationContext.getBean(DatevImportService.class));
        registerBean(applicationContext.getBean(EingangsrechnungDao.class));
        registerBean(applicationContext.getBean(EmployeeDao.class));
        registerBean(applicationContext.getBean(EmployeeSalaryDao.class));
        registerBean(applicationContext.getBean(GroupDao.class));
        registerBean(applicationContext.getBean(HibernateSearchReindexer.class));
        registerBean(applicationContext.getBean(JCRCheckSanityCheckJob.class));
        registerBean(applicationContext.getBean(Kost1Dao.class));
        registerBean(applicationContext.getBean(Kost2Dao.class));
        registerBean(applicationContext.getBean(KostCache.class));
        registerBean(applicationContext.getBean(MenuCreator.class));
        registerBean(applicationContext.getBean(ProjektDao.class));
        registerBean(applicationContext.getBean(PluginAdminService.class));
        registerBean(applicationContext.getBean(RechnungDao.class));
        registerBean(applicationContext.getBean(SipgateConfiguration.class));
        //WicketSupport.getInstance().registerBean(applicationContext.getBean(SipgateDirectCallService.class));
        registerBean(applicationContext.getBean(SmsSenderConfig.class));
        registerBean(applicationContext.getBean(SystemStatus.class));
        registerBean(applicationContext.getBean(TaskDao.class));
        registerBean(applicationContext.getBean(TaskTree.class));
        registerBean(applicationContext.getBean(TeamEventDao.class));
        registerBean(applicationContext.getBean(TimesheetDao.class));
        registerBean(applicationContext.getBean(UserDao.class));
        registerBean(applicationContext.getBean(UserPrefCache.class));
        registerBean(applicationContext.getBean(UserRightDao.class));
        registerBean(applicationContext.getBean(UserService.class));
        registerBean(applicationContext.getBean(UserXmlPreferencesDao.class));
        registerBean(applicationContext.getBean(UserXmlPreferencesMigrationDao.class));
    }
}
