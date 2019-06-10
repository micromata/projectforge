import { faCheck } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { components } from 'react-select';
import CalendarStyler from '../../../../containers/panel/calendar/CalendarStyler';
import { getServiceURL } from '../../../../utilities/rest';
import { Button } from '../../../design';
import Input from '../../../design/input';
import Popper from '../../../design/popper';

function EditableMultiValueLabel({ data, selectProps, ...props }) {
    const initialValue = selectProps.values[data.id] || '';

    const [isOpen, setIsOpen] = React.useState(false);
    const [value, setValue] = React.useState(initialValue);

    const popperRef = React.useRef(null);

    // Close Popper when clicking outside
    React.useEffect(() => {
        const handleMouseClick = (event) => {
            if (popperRef.current && !popperRef.current.parentElement.contains(event.target)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('click', handleMouseClick);

        return () => document.removeEventListener('click', handleMouseClick);
    });

    let input;
    let { label } = data;

    // disable eslint because variable is provided by react-select and can't be changed.
    /* eslint-disable-next-line no-underscore-dangle */
    if (!data.__isNew__) {
        label = `${label}${initialValue ? `: ${initialValue}` : ''}`;
    }

    // Function to set value in react-select
    const submitValue = () => {
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
                        .catch(error => alert(`Internal error: ${error}`));
                }
                break;
            default:
        }

        setIsOpen(false);
        selectProps.setMultiValue(data.id, value);
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
                <CalendarStyler calendar={data} submit={submitValue}/>
            );
            break;
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
                    <FontAwesomeIcon icon={faCheck}/>
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
