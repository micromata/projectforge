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

package org.projectforge.business.scripting;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.lang.Writable;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.projectforge.framework.access.AccessException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Executes groovy templates. For more functionality please refer GroovyEngine.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
public class GroovyExecutor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GroovyExecutor.class);

    public ScriptExecutionResult execute(final ScriptExecutionResult result, final String script, final Map<String, Object> variables, final ScriptLogger scriptLogger) {
        if (script == null) {
            return result;
        }
        final Script groovyObject = compileGroovy(result, script, true);
        if (groovyObject == null) {
            return result;
        }
        return execute(result, groovyObject, variables, scriptLogger);
    }

    public String executeTemplate(final String template, final Map<String, Object> variables) {
        securityChecks(template);
        return executeTemplate(new SimpleTemplateEngine(), template, variables);
    }

    public String executeTemplate(final TemplateEngine templateEngine, final String template,
                                  final Map<String, Object> variables) {
        securityChecks(template);
        if (template == null) {
            return null;
        }
        try {
            final Template templateObject = templateEngine.createTemplate(template);
            final Writable writable = templateObject.make(variables);
            final StringWriter writer = new StringWriter();
            writable.writeTo(writer);
            writer.flush();
            if (log.isDebugEnabled()) {
                log.debug(writer.toString());
            }
            return writer.toString();
        } catch (final CompilationFailedException | IOException | ClassNotFoundException ex) {
            log.error(ex.getMessage() + " while executing template: " + template, ex);
        }
        return null;
    }

    /**
     * @param script
     * @param bindScriptResult If true then "scriptResult" from type GroovyResult is binded.
     * @return
     */
    public Script compileGroovy(final String script, final boolean bindScriptResult) {
        return compileGroovy(null, script, bindScriptResult);
    }

    /**
     * @param script
     * @param bindScriptResult If true then "scriptResult" from type GroovyResult is binded.
     * @return
     */
    public Script compileGroovy(final ScriptExecutionResult result, final String script, final boolean bindScriptResult) {
        securityChecks(script);
        final GroovyClassLoader gcl = new GroovyClassLoader() {
            @SuppressWarnings("rawtypes")
            @Override
            public Class loadClass(String name, boolean lookupScriptFiles, boolean preferClassOverScript, boolean resolve)
                    throws ClassNotFoundException, CompilationFailedException {
                Class loadClass = null;
                try {
                    loadClass = super.loadClass(name, lookupScriptFiles, preferClassOverScript, resolve);
                } catch (ClassNotFoundException e) {
                    if (name.startsWith("org.projectforge")) {
                        log.error("Error while resolving Class: " + name);
                    }
                }
                return loadClass;
            }
        };

        Class<?> groovyClass;
        try {
            groovyClass = gcl.parseClass(script);
        } catch (final CompilationFailedException ex) {
            log.info("Groovy-CompilationFailedException: " + ex.getMessage());
            if (result != null) {
                result.setException(ex);
            }
            return null;
        }
        Script groovyObject;
        try {
            groovyObject = (Script) groovyClass.newInstance();
        } catch (final InstantiationException | IllegalAccessException ex) {
            log.error(ex.getMessage(), ex);
            if (result != null) {
                result.setException(ex);
            }
            return null;
        }
        if (bindScriptResult) {
            final Binding binding = groovyObject.getBinding();
            binding.setVariable("scriptResult", result);
        }
        return groovyObject;
    }

    public ScriptExecutionResult execute(final Script groovyScript, final ScriptLogger scriptLogger) {
        return execute(groovyScript, null, scriptLogger);
    }

    public ScriptExecutionResult execute(final Script groovyScript, final Map<String, Object> variables, final ScriptLogger scriptLogger) {
        return execute(null, groovyScript, variables, scriptLogger);
    }

    public ScriptExecutionResult execute(ScriptExecutionResult result, final Script groovyScript, final Map<String, Object> variables, final ScriptLogger scriptLogger) {
        if (variables != null) {
            final Binding binding = groovyScript.getBinding();
            for (final Map.Entry<String, Object> entry : variables.entrySet()) {
                binding.setVariable(entry.getKey(), entry.getValue());
            }
        }
        if (result == null) {
            result = new ScriptExecutionResult(scriptLogger);
        }
        scriptLogger.info(I18n.getString("scripting.script.execution.log.started"));
        Object res;
        try {
            res = groovyScript.run();
        } catch (final Exception ex) {
            log.info("Groovy-Execution-Exception: " + ex.getMessage(), ex);
            result.setException(ex);
            return result;
        }
        result.setResult(res);
        return result;
    }

    /**
     * Better than nothing...
     *
     * @param script
     */
    private void securityChecks(final String script) {
        final String[] forbiddenKeyWords = {"__baseDao", "__baseObject", "System.ex"};
        for (final String forbiddenKeyWord : forbiddenKeyWords) {
            if (StringUtils.contains(script, forbiddenKeyWord)) {
                throw new AccessException("access.exception.violation", forbiddenKeyWord);
            }
        }
    }
}
