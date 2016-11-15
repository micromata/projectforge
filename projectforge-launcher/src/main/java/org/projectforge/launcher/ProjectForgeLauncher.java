package org.projectforge.launcher;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
@SpringBootApplication(scanBasePackages = { "org.projectforge", "de.micromata.mgc.jpa.spring", "de.micromata.mgc.springbootapp" })
@ServletComponentScan("org.projectforge.web")
@PropertySource("file:projectforge.properties")
public class ProjectForgeLauncher
{
}
