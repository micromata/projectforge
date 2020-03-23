import { faCheckSquare } from '@fortawesome/free-solid-svg-icons';
import PropTypes from 'prop-types';
import React from 'react';
import { Input } from '../../../components/design';
import style from '../../../components/design/input/Input.module.scss';

function FavoriteNameInput(
    {
        defaultValue,
        id,
        label,
        onSave,
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
        <React.Fragment>
            <Input
                label={label}
                icon={faCheckSquare}
                iconProps={{
                    size: 'lg',
                    onClick: handleCreateClick,
                }}
                id={id}
                onChange={handleInputChange}
                onKeyDown={handleKeyDown}
                value={filterName}
                {...props}
            />
        </React.Fragment>
    );
}

FavoriteNameInput.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onSave: PropTypes.func.isRequired,
    defaultValue: PropTypes.string,
};

FavoriteNameInput.defaultProps = {
    defaultValue: '',
};

export default FavoriteNameInput;
