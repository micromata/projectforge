import PropTypes from 'prop-types';
import React from 'react';
import Input from '../../../../../../components/design/input';

function MagicStringInput(
    {
        label,
        id,
        onCancel,
        onChange,
        onSubmit,
        value,
    },
) {
    const handleKeyDown = ({ key }) => {
        switch (key) {
            case 'Enter':
                onSubmit();
                break;
            case 'Escape':
                onCancel();
                break;
            default:
        }
    };

    return (
        <Input
            autoFocus
            id={`magic-string-input-${id}`}
            label={label}
            onChange={({ target }) => onChange({ value: target.value })}
            onKeyDown={handleKeyDown}
            value={value.value || ''}
            style={{ width: 400 }}
        />
    );
}

MagicStringInput.propTypes = {
    label: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    onCancel: PropTypes.func.isRequired,
    onChange: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
    value: PropTypes.shape({}).isRequired,
};

MagicStringInput.defaultProps = {};

MagicStringInput.getLabel = (label, { value }) => `${label}: ${value}`;

export default MagicStringInput;
