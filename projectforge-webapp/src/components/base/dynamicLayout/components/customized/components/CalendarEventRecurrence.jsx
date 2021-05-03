import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import RRuleGenerator, { translations } from 'react-rrule-generator';
import 'react-rrule-generator/build/styles.css';
import { Col, Row } from 'reactstrap';
import ReactSelect from '../../../../../design/react-select/ReactSelect';
import { DynamicLayoutContext } from '../../../context';

function CalendarEventRecurrence({ locale }) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const options = [
        {
            value: 'NONE',
            label: ui.translations['common.recurrence.frequency.none'],
        },
        {
            value: 'YEARLY',
            label: ui.translations['common.recurrence.frequency.yearly'],
        },
        {
            value: 'MONTHLY',
            label: ui.translations['common.recurrence.frequency.monthly'],
        },
        {
            value: 'WEEKLY',
            label: ui.translations['common.recurrence.frequency.weekly'],
        },
        {
            value: 'DAILY',
            label: ui.translations['common.recurrence.frequency.daily'],
        },
        {
            value: 'CUSTOMIZED',
            label: ui.translations['plugins.teamcal.event.recurrence.customized'],
        },
    ];

    const onChange = (rrule) => setData({ recurrenceRule: rrule });

    const getTranslation = () => ((locale === 'de') ? translations.german : undefined);

    const [value, setValue] = React.useState(() => {
        const rrule = data.recurrenceRule && data.recurrenceRule.toUpperCase();

        if (!rrule || rrule.indexOf('FREQ') < 0) {
            return 'NONE';
        }

        if (rrule.indexOf('INTERVAL') >= 0 && rrule.indexOf('INTERVAL=1') < 0) {
            // value of interval isn't 1:
            return 'CUSTOMIZED';
        }

        if (rrule.indexOf('BY') >= 0
            || rrule.indexOf('UNTIL') >= 0
            || rrule.indexOf('COUNT') >= 0
            || rrule.indexOf('WKST') >= 0) {
            // customized options chosen:
            return 'CUSTOMIZED';
        }

        return options
            .map(({ value: opt }) => opt)
            .find((opt) => rrule.indexOf(opt) > 0) || 'CUSTOMIZED';
    });

    const onSelectChange = (option) => {
        const val = option.value;
        setValue(val);
        if (val === 'NONE') {
            setData({ recurrenceRule: '' });
        } else if (val === 'CUSTOMIZED') {
            // RRule will no be set by RRuleGenerator, nothing to-do.
        } else {
            // eslint-disable-next-line no-restricted-syntax
            for (const opt of options) {
                if (val === opt.value) {
                    setData({ recurrenceRule: `FREQ=${opt.value};INTERVAL=1` });
                    return;
                }
            }
        }
    };

    const defaultValue = options.find((element) => element.value === value) || options[0];
    return React.useMemo(
        () => (
            <>
                <Row>
                    <Col sm={4}>
                        <ReactSelect
                            label={ui.translations['plugins.teamcal.event.recurrence']}
                            translations={ui.translations}
                            values={options}
                            value={defaultValue}
                            onChange={onSelectChange}
                            required
                        />
                    </Col>
                </Row>
                {value === 'CUSTOMIZED'
                    ? (
                        <Row className="rrule-generator">
                            <Col sm={12}>
                                <RRuleGenerator
                                    onChange={(rrule) => onChange(rrule)}
                                    value={data.recurrenceRule}
                                    config={{
                                        repeat: ['Yearly', 'Monthly', 'Weekly', 'Daily'],
                                    }}
                                    translations={getTranslation()}
                                />
                            </Col>
                        </Row>
                    ) : undefined}
            </>
        ),
        [value, setData],
    );
}

CalendarEventRecurrence.propTypes = {
    locale: PropTypes.string,
};

CalendarEventRecurrence.defaultProps = {
    locale: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(CalendarEventRecurrence);
