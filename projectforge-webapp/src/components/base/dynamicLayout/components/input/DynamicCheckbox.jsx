import PropTypes from 'prop-types';
import React from 'react';
import CheckBox from '../../../../design/input/CheckBox';
import ValidationManager from '../../../../design/input/ValidationManager';
import { DynamicLayoutContext } from '../../context';

function DynamicCheckbox({ id, label }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        const handleCheckboxChange = ({ target }) => setData({ [id]: target.checked });

        return (
            <ValidationManager>
                <CheckBox
                    id={id}
                    label={label}
                    checked={data[id] || false}
                    onChange={handleCheckboxChange}
                />
            </ValidationManager>
        );
    }, [data[id]]);
}

DynamicCheckbox.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
};

DynamicCheckbox.defaultProps = {};

export default DynamicCheckbox;
