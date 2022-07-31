import { faFolderPlus } from '@fortawesome/free-solid-svg-icons';
import PropTypes from 'prop-types';
import React from 'react';
import { Input } from '../../../components/design';

function FavoriteNameInput(
    {
        defaultValue,
        id,
        label,
        onSave,
        tooltip,
        ...props
    },
) {
    const [filterName, setFilterName] = React.useState(defaultValue);

    const handleInputChange = ({ target }) => setFilterName(target.value);

    const handleCreateClick = () => onSave(filterName);

    const handleKeyDown = ({ key }) => {
        if (key === 'Enter') {
            onSave(filterName);
        }
    };

    return (
        <Input
            label={label}
            icon={faFolderPlus}
            iconProps={{
                size: 'lg',
                onClick: handleCreateClick,
            }}
            id={id}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            value={filterName}
            tooltip={tooltip}
            {...props}
        />
    );
}

FavoriteNameInput.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onSave: PropTypes.func.isRequired,
    className: PropTypes.string,
    defaultValue: PropTypes.string,
    tooltip: PropTypes.string,
};

FavoriteNameInput.defaultProps = {
    className: '',
    defaultValue: '',
    tooltip: undefined,
};

export default FavoriteNameInput;
