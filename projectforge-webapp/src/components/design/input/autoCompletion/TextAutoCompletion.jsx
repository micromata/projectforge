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
        value = '',
        focus = false,
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
            /* eslint-disable-next-line react/no-unstable-nested-components */
            input={({ ref, ...otherInputProps }) => (
                <AdvancedPopperInput
                    forwardRef={ref}
                    id={inputId}
                    autoFocus={focus}
                    {...otherInputProps}
                    {...inputProps}
                    onChange={handleChange}
                />
            )}
            onSelect={handleSelect}
            search={value}
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

export default TextAutoCompletion;
