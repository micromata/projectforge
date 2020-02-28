import timezone from 'moment-timezone';
import 'moment/min/locales';
import PropTypes from 'prop-types';
import 'rc-time-picker/assets/index.css';
import React from 'react';
import 'react-day-picker/lib/style.css';
import AdditionalLabel from '../../../../design/input/AdditionalLabel';
import TimeInput from '../../../../design/input/calendar/TimeInput';
import InputContainer from '../../../../design/input/InputContainer';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicTimestampInput(
    {
        additionalLabel,
        id,
        label,
    },
) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const dateStr = Object.getByString(data, id);
    const [date, setDate] = React.useState(undefined);

    React.useEffect(() => {
        setDate(dateStr ? timezone(dateStr) : undefined);
    }, [dateStr]);

    return React.useMemo(() => (
        <React.Fragment>
            {/* TODO: VALIDATION */}
            <DynamicValidationManager id={id}>
                <InputContainer
                    isActive
                    label={label}
                    style={{ display: 'flex' }}
                    withMargin
                >
                    <TimeInput
                        id={id}
                        setTime={newDate => setData({ [id]: newDate })}
                        showDate
                        time={date ? date.toDate() : undefined}
                    />
                </InputContainer>
            </DynamicValidationManager>
            <AdditionalLabel title={additionalLabel} />
        </React.Fragment>
    ), [date, setData]);
}

DynamicTimestampInput.propTypes = {
    id: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    label: PropTypes.string,
};

DynamicTimestampInput.defaultProps = {
    label: undefined,
};

export default DynamicTimestampInput;
