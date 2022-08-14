import React from 'react';
import PropTypes from 'prop-types';
import { DynamicLayoutContext } from '../../../context';
import DynamicValidationManager from '../../input/DynamicValidationManager';
import Input from '../../../../../design/input';

function CustomizedColorChooser({ values }) {
    const { label, id } = values;

    const { data, setData } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || '';

    const handleInputChange = ({ target }) => setData({ [id]: target.value });

    return React.useMemo(
        () => (
            <DynamicValidationManager id={id}>
                <Input
                    id={`color-${id}`}
                    onChange={handleInputChange}
                    type="text"
                    label={label}
                    value={value}
                />
            </DynamicValidationManager>
        ),
        [
            values.label,
            values.id,
            data,
        ],
    );
}

CustomizedColorChooser.propTypes = {
    values: PropTypes.shape({
        label: PropTypes.string.isRequired,
        id: PropTypes.string.isRequired,
    }).isRequired,
};

CustomizedColorChooser.defaultProps = {};

export default CustomizedColorChooser;
