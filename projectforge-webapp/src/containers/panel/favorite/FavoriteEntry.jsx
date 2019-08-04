import { faCheck, faEdit, faSync, faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import style from '../../../components/design/input/Input.module.scss';
import FavoriteActionButton from './FavoriteActionButton';
import FavoriteNameInput from './FavoriteNameInput';

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
    const [inEditMode, setInEditMode] = React.useState(false);
    const entryRef = React.useRef(null);

    const handleMouseClickEvent = ({ target }) => {
        if (!entryRef.current || entryRef.current.contains(target)) {
            return;
        }

        setInEditMode(false);
    };

    React.useEffect(() => {
        if (inEditMode) {
            document.addEventListener('click', handleMouseClickEvent);

            return () => document.removeEventListener('click', handleMouseClickEvent);
        }

        return () => {
        };
    }, [inEditMode]);

    const handleItemClick = () => {
        if (inEditMode) {
            return;
        }

        onFavoriteSelect(id, name);
    };

    const handleRenameClick = (event) => {
        event.preventDefault();
        setInEditMode(true);
    };
    const handleRenameComplete = (newName) => {
        onFavoriteRename(id, newName);

        setInEditMode(false);
    };

    const handleDeleteClick = () => onFavoriteDelete(id);
    const handleSyncClick = () => {
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
            ref={entryRef}
        >
            <span className={classNames({ [style.hidden]: !inEditMode })}>
                <FavoriteNameInput
                    defaultValue={name}
                    onSave={handleRenameComplete}
                    label={translations.rename}
                    id={`rename-favorite-input-${id}`}
                    autoFocus
                />
            </span>
            <span className={classNames({ [style.hidden]: inEditMode })}>
                <span className={style.favoriteName}>{name}</span>
                <div className={style.actions}>
                    {onFavoriteRename ? (
                        <FavoriteActionButton
                            icon={faEdit}
                            id={`rename-favorite-${id}`}
                            onClick={handleRenameClick}
                            tooltip={translations.rename}
                        />
                    ) : undefined}
                    {onFavoriteDelete ? (
                        <FavoriteActionButton
                            className={style.deleteIcon}
                            icon={faTrashAlt}
                            id={`delete-favorite-${id}`}
                            onClick={handleDeleteClick}
                            tooltip={translations.delete}
                        />
                    ) : undefined}
                    {onFavoriteUpdate && currentFavoriteId === id ? (
                        <FavoriteActionButton
                            className={style.syncIcon}
                            icon={isModified ? faSync : faCheck}
                            id={`syncFavoriteIcon-${id}`}
                            onClick={handleSyncClick}
                            tooltip={translations[isModified ? 'save' : 'uptodate']}
                        />
                    ) : undefined}
                </div>
            </span>
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
