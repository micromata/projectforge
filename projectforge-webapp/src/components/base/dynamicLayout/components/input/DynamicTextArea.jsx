import PropTypes from 'prop-types';
import React from 'react';
import TextArea from '../../../../design/input/TextArea';
import { DynamicLayoutContext } from '../../context';
import DynamicValidationManager from './DynamicValidationManager';

function DynamicTextArea({ id, focus, ...props }) {
    const { data, setData, ui } = React.useContext(DynamicLayoutContext);

    const value = Object.getByString(data, id) || '';

    return React.useMemo(() => {
        const handleTextAreaChange = ({ target }) => setData({ [id]: target.value });

        return (
            <DynamicValidationManager id={id}>
                <TextArea
                    id={`${ui.uid}-${id}`}
                    onChange={handleTextAreaChange}
                    autoFocus={focus}
                    // eslint-disable-next-line react/jsx-props-no-spreading
                    {...props}
                    value={value}
                />
            </DynamicValidationManager>
        );
    }, [value, setData, id, focus, props]);
}

DynamicTextArea.propTypes = {
    id: PropTypes.string.isRequired,
    focus: PropTypes.bool,
};

DynamicTextArea.defaultProps = {
    focus: false,
};

export default DynamicTextArea;
