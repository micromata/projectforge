import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import React from 'react';
import RRuleGenerator, { translations } from 'react-rrule-generator';
import { DynamicLayoutContext } from '../../../context';
import 'react-rrule-generator/build/styles.css';
import ReactSelect from '../../../../../design/ReactSelect';
import './CalendarEventRecurrency.module.css';

function CalendarEventRecurrency({ locale }) {
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
            if (rrule.indexOf('BY') >= 0 || rrule.indexOf('UNTIL') >= 0 || rrule.indexOf('COUNT') >= 0) {
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
                <ReactSelect
                    label={ui.translations['plugins.teamcal.event.recurrence']}
                    translations={ui.translations}
                    values={options}
                    defaultValue={defaultValue}
                    onChange={onSelectChange}
                    required
                />
                {value === 'CUSTOMIZED'
                    ? (
                        <div className="rrule-generator">
                            <RRuleGenerator
                                onChange={rrule => onChange(rrule)}
                                value={data.recurrenceRule}
                                config={{
                                    repeat: ['Yearly', 'Monthly', 'Weekly', 'Daily'],
                                }}
                                translations={getTranslation()}
                            />
                        </div>
                    ) : undefined}
            </React.Fragment>
        ),
        [value],
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
