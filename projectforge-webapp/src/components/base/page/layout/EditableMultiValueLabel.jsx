import { faCheck } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import { components } from 'react-select';
import { Button } from '../../../design';
import Input from '../../../design/input';
import Popper from '../../../design/popper';

const stopEventPropagation = event => event.stopPropagation();

function EditableMultiValueLabel({ data, selectProps, ...props }) {
    const initialValue = selectProps.values[data.id] || '';

    const [isOpen, setIsOpen] = React.useState(true);
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
            // TODO: IMPLEMENT COLOR PICKER
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

    // Function to set value in react-select
    const submitValue = () => {
        setIsOpen(false);
        selectProps.setMultiValue(data.id, value);
    };

    const selectHandler = {
        // Cancel all the react-select events
        onMouseDownCapture: stopEventPropagation,
        onMouseDown: stopEventPropagation,
        onClick: stopEventPropagation,
        onKeyDown: (event) => {
            stopEventPropagation(event);

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
