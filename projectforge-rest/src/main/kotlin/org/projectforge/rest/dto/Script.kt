package org.projectforge.rest.dto

import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptParameterType

class Script(
        var name: String? = null,
        var description: String? = null,
        var parameter1Name: String? = null,
        var parameter1Type: ScriptParameterType? = null,
        var parameter2Name: String? = null,
        var parameter2Type: ScriptParameterType? = null,
        var parameter3Name: String? = null,
        var parameter3Type: ScriptParameterType? = null,
        var parameter4Name: String? = null,
        var parameter4Type: ScriptParameterType? = null,
        var parameter5Name: String? = null,
        var parameter5Type: ScriptParameterType? = null,
        var parameter6Name: String? = null,
        var parameter6Type: ScriptParameterType? = null
) : BaseDTO<ScriptDO>() {
    var parameter: String? = null
}