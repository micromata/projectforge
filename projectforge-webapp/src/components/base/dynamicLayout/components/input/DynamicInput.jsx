import PropTypes from 'prop-types';
import React from 'react';
import Input from '../../../../design/input';
import ValidationManager from '../../../../design/input/ValidationManager';
import { DynamicLayoutContext } from '../../context';

function DynamicInput({ id, ...props }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    const handleInputChange = ({ target }) => setData({ [id]: target.value });

    return (
        <ValidationManager>
            <Input
                id={id}
                onChange={handleInputChange}
                {...props}
                value={data[id] || ''}
            />
        </ValidationManager>
    );
}

DynamicInput.propTypes = {
    id: PropTypes.string.isRequired,
};

export default DynamicInput;
