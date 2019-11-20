import PropTypes from 'prop-types';
import React from 'react';
import Input from '../../../../design/input';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicInput({ id, focus, ...props }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || '';

    // Only rerender input when data has changed
    return React.useMemo(() => {
        const handleInputChange = ({ target }) => setData({ [id]: target.value });

        return (
            <DynamicValidationManager id={id}>
                <Input
                    id={id}
                    onChange={handleInputChange}
                    autoFocus={focus}
                    {...props}
                    value={value}
                />
            </DynamicValidationManager>
        );
    }, [value, setData]);
}

DynamicInput.propTypes = {
    id: PropTypes.string.isRequired,
    focus: PropTypes.bool,
};

DynamicInput.defaultProps = {
    focus: false,
};

export default DynamicInput;
