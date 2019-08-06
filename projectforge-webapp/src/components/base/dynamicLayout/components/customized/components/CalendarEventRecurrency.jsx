import React from 'react';
import RRuleGenerator from 'react-rrule-generator';
import { DynamicLayoutContext } from '../../../context';
import 'react-rrule-generator/build/styles.css';

function CalendarEventRecurrency() {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const onChange = rrule => setData({ recurrenceRule: rrule });

    const { translations } = ui;
    translations['days.weekend day'] = translations['days.weekendday']; // none Java format.


    return React.useMemo(
        () => (
            <React.Fragment>
                <RRuleGenerator
                    onChange={rrule => onChange(rrule)}
                    value={data.recurrenceRule}
                    config={{
                        repeat: ['Yearly', 'Monthly', 'Weekly', 'Daily'],
                    }}
                    translations={translations}
                />
            </React.Fragment>
        ),
        [data.recurrencRule],
    );
}

CalendarEventRecurrency.propTypes = {};

CalendarEventRecurrency.defaultProps = {};

export default CalendarEventRecurrency;
