import { faCheck } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { components } from 'react-select';
import { Button, ButtonGroup } from '.';
import { CalendarContext } from '../../containers/page/calendar/CalendarContext';
import CalendarStyler from '../../containers/panel/calendar/CalendarStyler';
import { useClickOutsideHandler } from '../../utilities/hooks';
import { getServiceURL, handleHTTPErrors } from '../../utilities/rest';
import Input from './input';
import DateTimeRange from './input/calendar/DateTimeRange';
import FormattedTimeRange from './input/calendar/FormattedTimeRange';
import Popper from './popper';

function EditableMultiValueLabel({ data, selectProps, ...props }) {
    const initialValue = selectProps.values[data.id] || '';

    const { saveUpdateResponseInState } = React.useContext(CalendarContext);

    const [isOpen, setIsOpen] = React.useState(data.isNew);
    const [value, setValue] = React.useState(initialValue);

    const popperRef = React.useRef(null);

    // Close Popper when clicking outside
    useClickOutsideHandler(popperRef, setIsOpen, isOpen);

    let input;
    let { label } = data;

    // disable eslint because variable is provided by react-select and can't be changed.
    /* eslint-disable-next-line no-underscore-dangle */
    if (!data.__isNew__) {
        label = `${label}${initialValue ? `: ${initialValue}` : ''}`;
    }

    // Function to set value in react-select
    const submitValue = () => {
        let sValue = value;

        switch (data.filterType) {
            case 'COLOR_PICKER':
                if (data.id && data.bgColor) {
                    fetch(getServiceURL('calendar/changeStyle', {
                        calendarId: data.id,
                        bgColor: data.bgColor,
                    }), {
                        method: 'GET',
                        credentials: 'include',
                    })
                        .then(handleHTTPErrors)
                        .then(response => response.json())
                        .then(saveUpdateResponseInState)
                        .catch(error => alert(`Internal error: ${error}`));
                }
                break;
            case 'SELECT':
                sValue = sValue.join(',');
                break;
            default:
        }

        setIsOpen(false);
        selectProps.setMultiValue(data.id, sValue);
    };

    // Handle Different Types of Filters
    switch (data.filterType) {
        case 'STRING':
            input = (
                <Input
                    label={data.label}
                    id={`editable-multi-value-input-${data.id}`}
                    value={value}
                    onChange={event => setValue(event.target.value)}
                    autoFocus
                />
            );
            break;
        case 'COLOR_PICKER':
            input = (
                <CalendarStyler calendar={data} submit={submitValue} />
            );
            break;
        case 'SELECT':
            // TODO CONVERT STRING TO ARRAY
            if (!Array.isArray(value)) {
                setValue([]);
            } else if (value.length !== 0) {
                label = `${data.label}: ${value
                // Find Labels for selected items by values
                    .map(v => data.values.find(dv => dv.value === v).label)
                    .join(', ')}`;
            }

            input = (
                <ButtonGroup>
                    {data.values.map(selectValue => (
                        <Button
                            key={`multi-value-${data.key}-${selectValue.value}`}
                            onClick={() => {
                                if (value.includes(selectValue.value)) {
                                    setValue(value.filter(v => v !== selectValue.value));
                                } else {
                                    setValue([...value, selectValue.value]);
                                }
                            }}
                            active={value.includes(selectValue.value)}
                        >
                            {selectValue.label}
                        </Button>
                    ))}
                </ButtonGroup>
            );
            break;
        case 'TIME_STAMP': {
            if (!Object.isObject(value)) {
                setValue({
                    from: undefined,
                    to: undefined,
                });
            } else if (value.from && value.to) {
                label = (
                    <FormattedTimeRange
                        childrenAsPrefix
                        id={`editable-multi-value-time-${data.id}`}
                        from={value.from}
                        to={value.to}
                    >
                        {`${data.label}: `}
                    </FormattedTimeRange>
                );
            }

            const setFrom = from => setValue({
                ...value,
                from,
            });
            const setTo = to => setValue({
                ...value,
                to,
            });

            input = (
                <React.Fragment>
                    <DateTimeRange
                        id={data.id}
                        onChange={setValue}
                        {...value}
                        setFrom={setFrom}
                        setTo={setTo}
                        selectors={[
                            'YEAR',
                            'MONTH',
                            'WEEK',
                            'DAY',
                            'UNTIL_NOW',
                        ]}
                    />
                </React.Fragment>
            );
            break;
        }
        // Case for plain searchString without filterType
        case undefined:
            return (
                <components.MultiValueLabel data={data} selectProps={selectProps} {...props} />
            );
        // Fallback for not implemented filterType
        default:
            input = `${data.filterType} is not implemented yet.`;
    }

    const selectHandler = {
        onClick: event => event.stopPropagation(),
        onMouseDown: event => event.stopPropagation(),
        onKeyDown: (event) => {
            event.stopPropagation();

            switch (event.key) {
                case 'Escape':
                    setIsOpen(false);
                    break;
                case 'Enter':
                    submitValue();
                    break;
                default:
            }
        },
    };

    return (
        <Popper
            isOpen={isOpen}
            {...selectHandler}
            target={(
                <div
                    onClick={() => setIsOpen(!isOpen)}
                    role="button"
                    tabIndex={-1}
                    onKeyPress={() => {
                    }}
                >
                    <components.MultiValueLabel
                        data={data}
                        selectProps={selectProps}
                        {...props}
                    >
                        {label}
                    </components.MultiValueLabel>
                </div>
            )}
        >
            <div ref={popperRef}>
                {input}
                <Button color="success" block onClick={submitValue}>
                    <FontAwesomeIcon icon={faCheck} />
                </Button>
            </div>
        </Popper>
    );
}

EditableMultiValueLabel.propTypes = {
    data: PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
        filterType: PropTypes.string,
    }).isRequired,
    selectProps: PropTypes.shape({
        setMultiValue: PropTypes.func,
    }).isRequired,
};

export default EditableMultiValueLabel;
