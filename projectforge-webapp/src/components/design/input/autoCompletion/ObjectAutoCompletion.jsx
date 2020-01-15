import PropTypes from 'prop-types';
import React from 'react';
import AdvancedPopperInput from '../../popper/AdvancedPopperInput';
import AutoCompletion from './index';

function ObjectAutoCompletion(
    {
        inputId,
        inputProps,
        onSelect,
        value,
        ...props
    },
) {
    const [search, setSearch] = React.useState('');

    React.useEffect(() => {
        if (search !== value.displayName) {
            setSearch(value.displayName);
        }
    }, [value.displayName]);

    const handleChange = ({ target }) => setSearch(target.value);

    return (
        <AutoCompletion
            input={({ ref, ...otherInputsProps }) => (
                <AdvancedPopperInput
                    forwardRef={ref}
                    id={inputId}
                    {...otherInputsProps}
                    {...inputProps}
                    onChange={handleChange}
                />
            )}
            onSelect={onSelect}
            search={search}
            {...props}
        />
    );
}

ObjectAutoCompletion.propTypes = {
    inputId: PropTypes.string.isRequired,
    onSelect: PropTypes.func.isRequired,
    icon: PropTypes.shape({}),
    inputProps: PropTypes.shape({}),
    value: PropTypes.shape({}),
};

ObjectAutoCompletion.defaultProps = {
    icon: undefined,
    inputProps: undefined,
    value: {},
};

export default ObjectAutoCompletion;
