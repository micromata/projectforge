import 'moment/min/locales';
import PropTypes from 'prop-types';
import React from 'react';
import 'react-day-picker/lib/style.css';
import AdditionalLabel from '../../../../design/input/AdditionalLabel';
import DateInput from '../../../../design/input/calendar/DateInput';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicDateInput(props) {
    const {
        additionalLabel,
        id,
        label,
        required,
    } = props;
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);
    let value = Object.getByString(data, id);

    // Might be not work in if time zone of browser differs from PF-Users timezone:
    // moment.tz.setDefault(timeZone);
    return React.useMemo(() => {
        if (typeof value === 'string') {
            value = new Date(value);
        }

        const handleDateChange = newDate => setData({ [id]: newDate });

        // TODO: VALIDATION
        return (
            <React.Fragment>
                <DynamicValidationManager id={id}>
                    <DateInput
                        date={value}
                        label={label}
                        required={required}
                        setDate={handleDateChange}
                        todayButton={ui.translations['calendar.today']}
                    />
                </DynamicValidationManager>
                {additionalLabel && (
                    <AdditionalLabel title={additionalLabel} />
                )}
            </React.Fragment>
        );
    }, [props, value, setData]);
}

DynamicDateInput.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    required: PropTypes.bool,
};

DynamicDateInput.defaultProps = {
    additionalLabel: undefined,
    required: false,
};

export default DynamicDateInput;
