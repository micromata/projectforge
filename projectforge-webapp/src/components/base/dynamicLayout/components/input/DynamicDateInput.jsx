import 'moment/min/locales';
import PropTypes from 'prop-types';
import React from 'react';
import 'react-day-picker/src/style.css';
import DateInput from '../../../../design/input/calendar/DateInput';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicDateInput({
    additionalLabel,
    id,
    label,
    required = false,
}) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    let value = Object.getByString(data, id);

    // Might be not work in if time zone of browser differs from PF-Users timezone:
    // moment.tz.setDefault(timeZone);
    return React.useMemo(() => {
        if (typeof value === 'string') {
            value = new Date(value);
        }

        const handleDateChange = (newDate) => setData({ [id]: newDate });

        return (
            <DynamicValidationManager id={id}>
                <DateInput
                    additionalLabel={additionalLabel}
                    label={label}
                    required={required}
                    setDate={handleDateChange}
                    todayButton={ui.translations['calendar.today']}
                    value={value}
                />
            </DynamicValidationManager>
        );
    }, [additionalLabel, id, label, required, value, setData]);
}

DynamicDateInput.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    required: PropTypes.bool,
};

export default DynamicDateInput;
