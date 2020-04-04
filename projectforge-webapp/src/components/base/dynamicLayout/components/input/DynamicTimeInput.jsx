import 'moment/min/locales';
import PropTypes from 'prop-types';
import 'rc-time-picker/assets/index.css';
import React from 'react';
import InputContainer from '../../../../design/input/InputContainer';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';
import TimeInput from '../../../../design/input/calendar/TimeInput';

function DynamicTimestampInput(
    {
        id,
        ...props
    },
) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const timeStr = Object.getByString(data, id);
    const [time, setTime] = React.useState([0, 0]);


    React.useEffect(() => {
        const timeTuple = timeStr && timeStr.split(':').map(part => Number(part));
        setTime(timeTuple && timeTuple.length >= 2 ? timeTuple.slice(0, 2) : [0, 0]);
    }, [timeStr]);

    const handleChange = (newTime) => {
        setData({ [id]: `${newTime.join(':')}:00` });
    };

    return React.useMemo(() => (
        <React.Fragment>
            {/* TODO: VALIDATION */}
            <DynamicValidationManager id={id}>
                <InputContainer
                    isActive
                    style={{ display: 'flex' }}
                    withMargin
                    id={`${ui.uid}-${id}`}
                    {...props}
                >
                    <TimeInput
                        id={id}
                        value={time}
                        onChange={handleChange}
                    />
                </InputContainer>
            </DynamicValidationManager>
        </React.Fragment>
    ), [time, setData]);
}

DynamicTimestampInput.propTypes = {
    id: PropTypes.string.isRequired,
};

DynamicTimestampInput.defaultProps = {};

export default DynamicTimestampInput;
