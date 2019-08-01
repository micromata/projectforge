package org.projectforge.plugins.marketing.dto

import org.projectforge.plugins.marketing.AddressCampaignDO
import org.projectforge.rest.dto.BaseDTO
import java.util.*

class AddressCampaign(id: Int?,
                      deleted: Boolean,
                      created: Date?,
                      lastUpdate: Date?,
                      tenantId: Int?)
    : BaseDTO<AddressCampaignDO>(id, deleted, created, lastUpdate, tenantId) {
}