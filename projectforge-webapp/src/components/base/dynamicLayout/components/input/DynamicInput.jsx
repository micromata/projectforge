import PropTypes from 'prop-types';
import React from 'react';
import Input from '../../../../design/input';
import ValidationManager from '../../../../design/input/ValidationManager';
import { DynamicLayoutContext } from '../../context';

function DynamicInput({ id, focus, ...props }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    // Only rerender input when data has changed
    return React.useMemo(() => {
        const handleInputChange = ({ target }) => setData({ [id]: target.value });

        return (
            <ValidationManager>
                <Input
                    id={id}
                    onChange={handleInputChange}
                    autoFocus={focus}
                    {...props}
                    value={data[id] || ''}
                />
            </ValidationManager>
        );
    }, [data[id]]);
}

DynamicInput.propTypes = {
    id: PropTypes.string.isRequired,
    focus: PropTypes.bool,
};

DynamicInput.defaultProps = {
    focus: false,
};

export default DynamicInput;
