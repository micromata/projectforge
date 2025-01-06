import timezone from 'moment-timezone';
import 'moment/min/locales';
import PropTypes from 'prop-types';
import 'rc-time-picker/assets/index.css';
import React from 'react';
import 'react-day-picker/dist/style.css';
import DateTimeInput from '../../../../design/input/calendar/DateTimeInput';
import InputContainer from '../../../../design/input/InputContainer';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicTimestampInput(
    {
        id,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const dateStr = Object.getByString(data, id);
    const [date, setDate] = React.useState(undefined);

    React.useEffect(() => {
        setDate(dateStr ? timezone(dateStr) : undefined);
    }, [dateStr]);

    return React.useMemo(() => (
        <>
            {/* TODO: VALIDATION */}
            <DynamicValidationManager id={id}>
                <InputContainer
                    isActive
                    style={{ display: 'flex' }}
                    withMargin
                    {...props}
                >
                    <DateTimeInput
                        id={`${ui.uid}-${id}`}
                        setTime={(newDate) => setData({ [id]: newDate })}
                        showDate
                        time={date ? date.toDate() : undefined}
                    />
                </InputContainer>
            </DynamicValidationManager>
        </>
    ), [date, setData, id, props]);
}

DynamicTimestampInput.propTypes = {
    id: PropTypes.string.isRequired,
};

export default DynamicTimestampInput;
