import PropTypes from 'prop-types';
import React from 'react';
import Input from '../../../../design/input';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicInput({ id, focus, ...props }) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || '';

    // Only rerender input when data has changed
    return React.useMemo(() => {
        const handleInputChange = ({ target }) => setData({ [id]: target.value });

        return (
            <DynamicValidationManager id={id}>
                <Input
                    id={`${ui.uid}-${id}`}
                    onChange={handleInputChange}
                    autoFocus={focus}
                    type="text"
                    {...props}
                    value={value}
                />
            </DynamicValidationManager>
        );
    }, [value, setData, id, focus, props]);
}

DynamicInput.propTypes = {
    id: PropTypes.string.isRequired,
    focus: PropTypes.bool,
};

DynamicInput.defaultProps = {
    focus: false,
};

export default DynamicInput;
