-- Constraints for t_plugin_marketing_address_campaign_value where missing

ALTER TABLE t_plugin_marketing_address_campaign_value ALTER COLUMN address_fk SET NOT NULL;
ALTER TABLE t_plugin_marketing_address_campaign_value ALTER COLUMN address_campaign_fk SET NOT NULL;
