import React from 'react';
import RRuleGenerator from 'react-rrule-generator';
import { DynamicLayoutContext } from '../../../context';
import 'react-rrule-generator/build/styles.css';

function CalendarEventRecurrency() {
    const { data, ui } = React.useContext(DynamicLayoutContext);

    return React.useMemo(
        () => {
            return (
                <RRuleGenerator
                    onChange={(rrule) => console.log(`RRule changed, now it's ${rrule}`)}
                    value={data.recurrenceRule}
                    //translations={ui.translations}
                />
            );
        },
        [data.recurrencRule],
    );
}

CalendarEventRecurrency.propTypes = {};

CalendarEventRecurrency.defaultProps = {};

export default CalendarEventRecurrency;
