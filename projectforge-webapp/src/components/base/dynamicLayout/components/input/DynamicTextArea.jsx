import PropTypes from 'prop-types';
import React from 'react';
import TextArea from '../../../../design/input/TextArea';
import ValidationManager from '../../../../design/input/ValidationManager';
import { DynamicLayoutContext } from '../../context';

function DynamicTextArea({ id, focus, ...props }) {
    const { data, setData } = React.useContext(DynamicLayoutContext);

    return React.useMemo(() => {
        const handleTextAreaChange = ({ target }) => setData({ [id]: target.value });

        return (
            <ValidationManager>
                <TextArea
                    id={id}
                    onChange={handleTextAreaChange}
                    autoFocus={focus}
                    {...props}
                    value={data[id] || ''}
                />
            </ValidationManager>
        );
    }, [data[id]]);
}

DynamicTextArea.propTypes = {
    id: PropTypes.string.isRequired,
    focus: PropTypes.bool,
};

DynamicTextArea.defaultProps = {
    focus: false,
};

export default DynamicTextArea;
