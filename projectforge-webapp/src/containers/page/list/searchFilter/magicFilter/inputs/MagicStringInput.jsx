import PropTypes from 'prop-types';
import React from 'react';
import Input from '../../../../../../components/design/input';

function MagicStringInput(
    {
        id,
        onCancel,
        onChange,
        onSubmit,
        translations,
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
            placeholder={translations.search || ''}
            onChange={({ target }) => onChange({ value: target.value })}
            onKeyDown={handleKeyDown}
            value={value.value || ''}
            style={{ width: 400 }}
        />
    );
}

MagicStringInput.propTypes = {
    id: PropTypes.string.isRequired,
    onCancel: PropTypes.func.isRequired,
    onChange: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
    translations: PropTypes.shape({
        search: PropTypes.string,
    }).isRequired,
    value: PropTypes.shape({
        value: PropTypes.shape({}),
    }).isRequired,
};

MagicStringInput.isEmpty = ({ value }) => !value || value.trim() === '';

MagicStringInput.getLabel = (label, { value }) => `${label}: ${value}`;

export default MagicStringInput;
