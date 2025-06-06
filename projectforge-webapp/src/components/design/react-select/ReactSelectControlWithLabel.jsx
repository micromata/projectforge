import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { components } from 'react-select';

function ReactSelectControlWithLabel(props) {
    const { selectProps, isFocused, hasValue } = props;

    const handleLabelClick = () => {
        if (selectProps.selectRef.current) {
            selectProps.selectRef.current.focus();
        }
    };

    return (
        <>
            <components.Control {...props} />
            <span
                className={classNames('react-select__label', { isActive: isFocused || hasValue })}
                onClick={handleLabelClick}
                onKeyDown={undefined}
                role="presentation"
            >
                {selectProps.label}
            </span>
        </>
    );
}

ReactSelectControlWithLabel.propTypes = {
    hasValue: PropTypes.bool.isRequired,
    isFocused: PropTypes.bool.isRequired,
    selectProps: PropTypes.shape({
        label: PropTypes.string,
        selectRef: PropTypes.shape({
            current: PropTypes.shape({
                focus: PropTypes.func,
            }),
        }),
    }).isRequired,
};

export default ReactSelectControlWithLabel;
