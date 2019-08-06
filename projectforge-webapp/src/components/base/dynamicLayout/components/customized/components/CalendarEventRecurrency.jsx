import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import React from 'react';
import RRuleGenerator, { translations } from 'react-rrule-generator';
import { DynamicLayoutContext } from '../../../context';
import 'react-rrule-generator/build/styles.css';
import ReactSelect from '../../../../../design/ReactSelect';

function CalendarEventRecurrency({ locale }) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const onChange = rrule => setData({ recurrenceRule: rrule });

    const extractRRule = () => {
        const rrule = data.recurrenceRule != null ? data.recurrenceRule.toLowerCase() : undefined;
        if (rrule && rrule.indexOf('freq') >= 0) {
            if (rrule.indexOf('interval') > 0 && rrule.indexOf('interval=1') < 0) {
                return 'customized';
            }
            if (rrule.indexOf('by') > 0) {
                return 'customized';
            }
            if (rrule.indexOf('yearly') > 0) {
                return 'yearly';
            }
            if (rrule.indexOf('monthly') > 0) {
                return 'monthly';
            }
            if (rrule.indexOf('weekly') > 0) {
                return 'weekly';
            }
            if (rrule.indexOf('daily') > 0) {
                return 'daily';
            }
            return 'customized';
        }
        return 'none';
    };

    const getTranslation = () => ((locale === 'de') ? translations.german : undefined);

    const [value, setValue] = React.useState(extractRRule());

    const onSelectChange = (valLabel) => {
        const val = valLabel.value;
        setValue(val);
        if (val === 'yearly') {
            setData({ recurrenceRule: 'FREQ=YEARLY;INTERVAL=1' });
        } else if (val === 'monthly') {
            setData({ recurrenceRule: 'FREQ=MONTHLY;INTERVAL=1' });
        } else if (val === 'weekly') {
            setData({ recurrenceRule: 'FREQ=WEEKLY;INTERVAL=1' });
        } else if (val === 'daily') {
            setData({ recurrenceRule: 'FREQ=DAILY;INTERVAL=1' });
        } else if (val === 'monthly') {
            setData({ recurrenceRule: 'FREQ=MONTHLY;INTERVAL=1' });
        } else if (val === 'none') {
            setData({ recurrenceRule: '' });
        }
    };

    const options = [
        {
            value: 'none',
            label: ui.translations['common.recurrence.frequency.none'],
        },
        {
            value: 'yearly',
            label: ui.translations['common.recurrence.frequency.yearly'],
        },
        {
            value: 'monthly',
            label: ui.translations['common.recurrence.frequency.monthly'],
        },
        {
            value: 'weekly',
            label: ui.translations['common.recurrence.frequency.weekly'],
        },
        {
            value: 'daily',
            label: ui.translations['common.recurrence.frequency.daily'],
        },
        {
            value: 'customized',
            label: ui.translations['plugins.teamcal.event.recurrence.customized'],
        },
    ];
    console.log(data.recurrenceRule);
    const defaultValue = options.find(element => element.value === value) || options[0];
    return React.useMemo(
        () => (
            <React.Fragment>
                <ReactSelect
                    label={ui.translations['plugins.teamcal.event.recurrence']}
                    translations={ui.translations}
                    values={options}
                    defaultValue={defaultValue}
                    onChange={onSelectChange}
                    required
                />
                {value === 'customized'
                    ? (
                        <RRuleGenerator
                            onChange={rrule => onChange(rrule)}
                            value={data.recurrenceRule}
                            config={{
                                repeat: ['Yearly', 'Monthly', 'Weekly', 'Daily'],
                            }}
                            translations={getTranslation()}
                        />
                    ) : undefined}
            </React.Fragment>
        ),
        [data.recurrencRule, value],
    );
}

CalendarEventRecurrency.propTypes = {
    locale: PropTypes.string,
};

CalendarEventRecurrency.defaultProps = {
    locale: undefined,
};

const mapStateToProps = ({ authentication }) => ({
    locale: authentication.user.locale,
});

export default connect(mapStateToProps)(CalendarEventRecurrency);
