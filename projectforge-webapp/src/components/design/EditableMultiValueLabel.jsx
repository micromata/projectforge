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
import AutoCompletion from './input/AutoCompletion';
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

    let popperContent;
    let { label } = data;
    let big = false;

    // disable eslint because variable is provided by react-select and can't be changed.
    /* eslint-disable-next-line no-underscore-dangle */
    if (!data.__isNew__) {
        label = `${label}${(initialValue && typeof initialValue === 'string') ? `: ${initialValue}` : ''}`;
    }

    // Function to set value in react-select
    const submitValue = () => {
        if (data.filterType === 'COLOR_PICKER' && data.id && data.bgColor) {
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

        setIsOpen(false);
        selectProps.setMultiValue(data.id, value);
    };

    // Handle Different Types of Filters
    switch (data.filterType) {
        case 'STRING':
            if (value.value) {
                label = `${data.label}: ${value.value}`;
            }

            popperContent = (
                <Input
                    label={data.label}
                    id={`editable-multi-value-input-${data.id}`}
                    value={value.value || ''}
                    onChange={({ target }) => setValue({ value: target.value })}
                    autoFocus
                />
            );
            break;
        case 'COLOR_PICKER':
            popperContent = (
                <CalendarStyler calendar={data} submit={submitValue} />
            );
            break;
        case 'SELECT': {
            const { values } = value;

            if (!Array.isArray(values)) {
                setValue({ values: [] });
                break;
            } else if (values.length !== 0) {
                label = `${data.label}: ${values
                // Find Labels for selected items by values
                    .map(v => data.values.find(dv => dv.value === v).label)
                    .join(', ')}`;
            }

            popperContent = (
                <ButtonGroup>
                    {data.values.map(selectValue => (
                        <Button
                            key={`multi-value-${data.key}-${selectValue.value}`}
                            onClick={() => {
                                if (values.includes(selectValue.value)) {
                                    setValue({
                                        values: values.filter(v => v !== selectValue.value),
                                    });
                                } else {
                                    setValue({ values: [...values, selectValue.value] });
                                }
                            }}
                            active={values.includes(selectValue.value)}
                        >
                            {selectValue.label}
                        </Button>
                    ))}
                </ButtonGroup>
            );
            break;
        }
        case 'TIME_STAMP': {
            big = true;

            if (value.to === undefined || value.from === undefined) {
                setValue({
                    from: null,
                    to: null,
                });
            } else if (typeof value.from === 'string' && typeof value.to === 'string') {
                setValue({
                    from: new Date(value.from),
                    to: new Date(value.to),
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

            // TODO CHECK IF FROM IS AFTER TO (AND VICE VERSA)
            const setFrom = from => setValue({
                ...value,
                from,
            });
            const setTo = to => setValue({
                ...value,
                to,
            });

            popperContent = (
                <React.Fragment>
                    <span className="text-info">{data.label}</span>
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
        case 'OBJECT': {
            big = true;

            const onChange = newValue => setValue(newValue);

            if (value && value.label) {
                label = `${data.label}: ${value.label}`;
            }

            popperContent = (
                <AutoCompletion
                    value={{
                        label: (value && value.label) || '',
                        value: (value && value.value) || '',
                    }}
                    id={`autocompletion-${data.id}`}
                    {...data.autoCompletion}
                    label={data.label}
                    onChange={onChange}
                />
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
            popperContent = `${data.filterType} is not implemented yet.`;
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
            direction="right"
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
            <div
                ref={popperRef}
                style={{ minWidth: Math.min(window.innerWidth - 64, big ? 700 : 350) }}
            >
                {popperContent}
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
