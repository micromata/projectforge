import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import React from 'react';
import { Col, Row } from 'reactstrap';
import RRuleGenerator, { translations } from 'react-rrule-generator';
import { DynamicLayoutContext } from '../../../context';
import 'react-rrule-generator/build/styles.css';
import ReactSelect from '../../../../../design/ReactSelect';

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

    const onChange = rrule => setData({ recurrenceRule: rrule });

    const initialValue = () => {
        const rrule = data.recurrenceRule != null ? data.recurrenceRule.toUpperCase() : undefined;
        if (rrule && rrule.indexOf('FREQ') >= 0) {
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
            // eslint-disable-next-line no-restricted-syntax
            for (const opt of options) {
                if (rrule.indexOf(opt.value) > 0) { // NONE and CUSTOMIZED shouldn't occur in rrule.
                    // Standard recurrency, e. g. FREQ=WEEKLY,INTERVAL=1
                    return opt.value;
                }
            }
            return 'CUSTOMIZED'; // Shouldn't occur, try then to handle this by RRuleGenerator.
        }
        return 'NONE';
    };

    const getTranslation = () => ((locale === 'de') ? translations.german : undefined);

    const [value, setValue] = React.useState(initialValue());

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

    const defaultValue = options.find(element => element.value === value) || options[0];
    return React.useMemo(
        () => (
            <React.Fragment>
                <Row>
                    <Col sm={4}>
                        <ReactSelect
                            label={ui.translations['plugins.teamcal.event.recurrence']}
                            translations={ui.translations}
                            values={options}
                            defaultValue={defaultValue}
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
                                    onChange={rrule => onChange(rrule)}
                                    value={data.recurrenceRule}
                                    config={{
                                        repeat: ['Yearly', 'Monthly', 'Weekly', 'Daily'],
                                    }}
                                    translations={getTranslation()}
                                />
                            </Col>
                        </Row>
                    ) : undefined}
            </React.Fragment>
        ),
        [value],
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
