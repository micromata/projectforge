import { faCheck, faEdit, faSync, faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { UncontrolledTooltip } from 'reactstrap';
import style from '../../../components/design/input/Input.module.scss';

function FavoriteEntry(
    {
        currentFavoriteId,
        id,
        isModified,
        name,
        onFavoriteDelete,
        onFavoriteRename,
        onFavoriteSelect,
        onFavoriteUpdate,
        translations,
    },
) {
    const handleItemClick = () => onFavoriteSelect(id, name);
    const handleRenameClick = (event) => {
        event.stopPropagation();
        // TODO CHANGE FIELD
        onFavoriteRename(id);
    };
    const handleDeleteClick = (event) => {
        event.stopPropagation();
        onFavoriteDelete(id);
    };
    const handleSyncClick = (event) => {
        event.stopPropagation();

        if (!(isModified && onFavoriteUpdate)) {
            return;
        }

        onFavoriteUpdate(id);
    };

    return (
        <li
            role="presentation"
            onClick={handleItemClick}
            className={classNames(style.favorite, { [style.selected]: id === currentFavoriteId })}
        >
            <span className={style.favoriteName}>{name}</span>
            <div className={style.actions}>
                {onFavoriteRename ? (
                    <React.Fragment>
                        <FontAwesomeIcon
                            id={`rename-favorite-${id}`}
                            icon={faEdit}
                            className={style.icon}
                            onClick={handleRenameClick}
                        />
                        <UncontrolledTooltip placement="right" target={`rename-favorite-${id}`}>
                            {translations.rename}
                        </UncontrolledTooltip>
                    </React.Fragment>
                ) : undefined}
                {onFavoriteDelete ? (
                    <React.Fragment>
                        <FontAwesomeIcon
                            id={`delete-favorite-${id}`}
                            icon={faTrashAlt}
                            className={classNames(style.icon, style.deleteIcon)}
                            onClick={handleDeleteClick}
                        />
                        <UncontrolledTooltip placement="right" target={`delete-favorite-${id}`}>
                            {translations.delete}
                        </UncontrolledTooltip>
                    </React.Fragment>
                ) : undefined}
                {currentFavoriteId === id ? (
                    <React.Fragment>
                        <FontAwesomeIcon
                            id="syncFavoriteIcon"
                            onClick={handleSyncClick}
                            icon={isModified ? faSync : faCheck}
                            className={classNames(style.icon, style.syncIcon)}
                        />
                        <UncontrolledTooltip placement="right" target="syncFavoriteIcon">
                            {translations[isModified ? 'save' : 'uptodate']}
                        </UncontrolledTooltip>
                    </React.Fragment>
                ) : undefined}
            </div>
        </li>
    );
}

FavoriteEntry.propTypes = {
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    onFavoriteSelect: PropTypes.func.isRequired,
    translations: PropTypes.shape({}).isRequired,
    currentFavoriteId: PropTypes.number,
    isModified: PropTypes.bool,
    onFavoriteDelete: PropTypes.func,
    onFavoriteRename: PropTypes.func,
    onFavoriteUpdate: PropTypes.func,
};

FavoriteEntry.defaultProps = {
    currentFavoriteId: 0,
    isModified: false,
    onFavoriteDelete: undefined,
    onFavoriteRename: undefined,
    onFavoriteUpdate: undefined,
};

export default FavoriteEntry;
