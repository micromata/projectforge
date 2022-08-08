/* eslint-disable no-alert */
import { faCheck } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { components } from 'react-select';
import { Button } from '.';
import { CalendarContext } from '../../containers/page/calendar/CalendarContext';
import CalendarStyler from '../../containers/panel/calendar/CalendarStyler';
import { useClickOutsideHandler } from '../../utilities/hooks';
import { getServiceURL, handleHTTPErrors } from '../../utilities/rest';
import Popper from './popper';

function EditableMultiValueLabel({
    data,
    selectProps,
    ...props
}) {
    const initialValue = selectProps.values[data.id] || '';

    const { saveUpdateResponseInState } = React.useContext(CalendarContext);

    const [isOpen, setIsOpen] = React.useState(data.isNew);

    const popperRef = React.useRef(null);

    // Close Popper when clicking outside
    useClickOutsideHandler(popperRef, setIsOpen, isOpen);

    let popperContent;
    let { label } = data;

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
                .then((response) => response.json())
                .then(saveUpdateResponseInState)
                .catch((error) => alert(`Internal error: ${error}`));
        }

        setIsOpen(false);
        selectProps.setMultiValue(data.id, initialValue);
    };

    // Handle Different Types of Filters
    switch (data.filterType) {
        case 'COLOR_PICKER':
            popperContent = (
                <CalendarStyler calendar={data} submit={submitValue} />
            );
            break;
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
        onClick: (event) => event.stopPropagation(),
        onMouseDown: (event) => event.stopPropagation(),
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
                    onClick={(event) => {
                        setIsOpen(!isOpen);
                        event.nativeEvent.stopPropagation();
                    }}
                    role="button"
                    tabIndex={-1}
                    onKeyPress={() => undefined}
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
                style={{
                    minWidth: Math.min(window.innerWidth - 64, 350),
                    padding: '10px 15px',
                }}
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
        label: PropTypes.string,
        bgColor: PropTypes.string,
        isNew: PropTypes.bool,
        __isNew__: PropTypes.bool,
    }).isRequired,
    selectProps: PropTypes.shape({
        setMultiValue: PropTypes.func,
        values: PropTypes.shape({}),
    }).isRequired,
};

export default EditableMultiValueLabel;
