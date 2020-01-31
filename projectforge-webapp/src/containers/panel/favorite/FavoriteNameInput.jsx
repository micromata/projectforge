import { faCheckSquare } from '@fortawesome/free-solid-svg-icons';
import PropTypes from 'prop-types';
import React from 'react';
import { Input } from '../../../components/design';
import style from '../../../components/design/input/Input.module.scss';
import FavoriteActionButton from './FavoriteActionButton';

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

    return (
        <React.Fragment>
            <Input
                label={label}
                id={id}
                onChange={handleInputChange}
                value={filterName}
                {...props}
            />
            <FavoriteActionButton
                className={style.saveIcon}
                icon={faCheckSquare}
                size="lg"
                onClick={handleCreateClick}
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
