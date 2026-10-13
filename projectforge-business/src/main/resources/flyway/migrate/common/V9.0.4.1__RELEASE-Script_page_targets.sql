-- Scripts may show a button on configured pages (page targets) with own label and tooltip:

ALTER TABLE t_script ADD COLUMN page_targets CHARACTER VARYING(1000);
ALTER TABLE t_script ADD COLUMN button_label CHARACTER VARYING(100);
ALTER TABLE t_script ADD COLUMN button_tooltip CHARACTER VARYING(1000);
