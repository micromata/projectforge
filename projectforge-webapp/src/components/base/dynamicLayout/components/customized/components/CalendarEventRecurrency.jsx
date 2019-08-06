import React from 'react';
import RRuleGenerator from 'react-rrule-generator';
import { DynamicLayoutContext } from '../../../context';
import 'react-rrule-generator/build/styles.css';

function CalendarEventRecurrency() {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const onChange = rrule => setData({ recurrenceRule: rrule });

    return React.useMemo(
        () => (
            <RRuleGenerator
                onChange={rrule => onChange(rrule)}
                value={data.recurrenceRule}
                //translations={ui.translations}
            />
        ),
        [data.recurrencRule],
    )
        ;
}

CalendarEventRecurrency.propTypes = {};

CalendarEventRecurrency.defaultProps = {};

export default CalendarEventRecurrency;
