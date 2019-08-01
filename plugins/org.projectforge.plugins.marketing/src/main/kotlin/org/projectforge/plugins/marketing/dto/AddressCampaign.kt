package org.projectforge.plugins.marketing.dto

import org.projectforge.plugins.marketing.AddressCampaignDO
import org.projectforge.rest.dto.BaseDTO
import java.util.*

class AddressCampaign(id: Int? = null,
                      deleted: Boolean = false,
                      created: Date? = null,
                      lastUpdate: Date? = null,
                      tenantId: Int? = null,
                      title: String? = null,
                      values: String? = null,
                      comment: String? = null)
    : BaseDTO<AddressCampaignDO>(id, deleted, created, lastUpdate, tenantId)