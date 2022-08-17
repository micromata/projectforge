import { faFolderPlus, faCheckSquare } from '@fortawesome/free-solid-svg-icons';
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
        rename,
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
    let icon = faFolderPlus;
    if (rename === true) {
        icon = faCheckSquare;
    }

    return (
        <Input
            label={label}
            icon={icon}
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
    rename: PropTypes.bool,
    className: PropTypes.string,
    defaultValue: PropTypes.string,
    tooltip: PropTypes.string,
};

FavoriteNameInput.defaultProps = {
    className: '',
    defaultValue: '',
    rename: false,
    tooltip: undefined,
};

export default FavoriteNameInput;
