import PropTypes from 'prop-types';
import React from 'react';
import AdvancedPopperInput from '../../popper/AdvancedPopperInput';
import AutoCompletion from './index';

function TextAutoCompletion(
    {
        inputId,
        inputProps,
        onChange,
        onSelect,
        value,
        focus,
        ...props
    },
) {
    const handleChange = ({ target }) => onChange(target.value);

    const handleSelect = (completion) => {
        if (onSelect) {
            onSelect(completion);
        } else {
            onChange(completion);
        }
    };

    return (
        <AutoCompletion
            input={({ ref, ...otherInputProps }) => (
                <AdvancedPopperInput
                    forwardRef={ref}
                    id={inputId}
                    autoFocus={focus}
                    // eslint-disable-next-line react/jsx-props-no-spreading
                    {...otherInputProps}
                    // eslint-disable-next-line react/jsx-props-no-spreading
                    {...inputProps}
                    onChange={handleChange}
                />
            )}
            onSelect={handleSelect}
            search={value}
            // eslint-disable-next-line react/jsx-props-no-spreading
            {...props}
        />
    );
}

TextAutoCompletion.propTypes = {
    inputId: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    inputProps: PropTypes.shape({}),
    onSelect: PropTypes.func,
    value: PropTypes.string,
    focus: PropTypes.bool,
};

TextAutoCompletion.defaultProps = {
    inputProps: undefined,
    onSelect: undefined,
    value: '',
    focus: false,
};

export default TextAutoCompletion;
