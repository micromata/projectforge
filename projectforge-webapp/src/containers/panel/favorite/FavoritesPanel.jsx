import PropTypes from 'prop-types';
import React from 'react';
import { Popover, PopoverBody } from '../../../components/design';
import style from '../../../components/design/input/Input.module.scss';
import FavoriteEntry from './FavoriteEntry';
import FavoriteNameInput from './FavoriteNameInput';
import FavoritesButton from './FavoritesButton';

function FavoritesPanel(
    {
        closeOnSelect,
        currentFavoriteId,
        htmlId,
        isModified,
        favorites,
        favoriteButtonText,
        onFavoriteCreate,
        onFavoriteDelete,
        onFavoriteRename,
        onFavoriteSelect,
        onFavoriteUpdate,
        translations,
    },
) {
    const [open, setOpen] = React.useState(false);
    const popperRef = React.useRef(null);

    const toggle = () => setOpen(!open);

    const handleFavoriteSelect = (id, name) => {
        if (closeOnSelect) {
            setOpen(false);
        }
        if (onFavoriteSelect) {
            onFavoriteSelect(id, name);
        }
    };

    const handleMouseClickEvent = ({ target }) => {
        if (!popperRef.current || popperRef.current.parentElement.contains(target)) {
            return;
        }

        setOpen(false);
    };

    React.useEffect(() => {
        if (open) {
            document.addEventListener('click', handleMouseClickEvent);

            return () => document.removeEventListener('click', handleMouseClickEvent);
        }

        return () => {
        };
    }, [open]);

    return (
        <React.Fragment>
            <FavoritesButton
                toggle={toggle}
                id={htmlId}
                isOpen={open}
                favoriteButtonText={favoriteButtonText}
            />
            <Popover
                placement="left-start"
                isOpen={open}
                target={htmlId}
                toggle={toggle}
                style={{
                    minWidth: 300,
                }}
            >
                <PopoverBody>
                    <ul className={style.favoritesList} ref={popperRef}>
                        {onFavoriteCreate ? (
                            <li className={style.addFavorite}>
                                <FavoriteNameInput
                                    id="newFilterName"
                                    onSave={onFavoriteCreate}
                                    label={translations['favorite.addNew'] || 'Add new'}
                                />
                            </li>
                        ) : undefined}
                        {favorites.map(favorite => (
                            <FavoriteEntry
                                key={favorite.id}
                                {...favorite}
                                currentFavoriteId={currentFavoriteId}
                                isModified={isModified}
                                onFavoriteDelete={onFavoriteDelete}
                                onFavoriteRename={onFavoriteRename}
                                onFavoriteSelect={handleFavoriteSelect}
                                onFavoriteUpdate={onFavoriteUpdate}
                                translations={translations}
                            />
                        ))}
                    </ul>
                </PopoverBody>
            </Popover>
        </React.Fragment>
    );
}

FavoritesPanel.propTypes = {
    onFavoriteSelect: PropTypes.func.isRequired,
    // Should the pop-over be closed after a favorite entry was selected?
    closeOnSelect: PropTypes.bool,
    // The current used favorite (can be saved with modified settings).
    currentFavoriteId: PropTypes.number,
    favoriteButtonText: PropTypes.string,
    favorites: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.any,
        name: PropTypes.string,
    })),
    htmlId: PropTypes.string,
    // Is true, if the current favorite filter is modified and is ready for update, otherwise false.
    // Default is false (so favorite can't be updated)
    isModified: PropTypes.bool,
    onFavoriteCreate: PropTypes.func,
    onFavoriteDelete: PropTypes.func,
    onFavoriteRename: PropTypes.func,
    onFavoriteUpdate: PropTypes.func,
    translations: PropTypes.shape({
        'favorite.addNew': PropTypes.string,
    }),
};

FavoritesPanel.defaultProps = {
    closeOnSelect: true,
    currentFavoriteId: -1,
    favoriteButtonText: undefined,
    favorites: [],
    htmlId: 'favoritesPopover',
    isModified: false,
    onFavoriteCreate: undefined,
    onFavoriteDelete: undefined,
    onFavoriteRename: undefined,
    onFavoriteUpdate: undefined,
    translations: {},
};

export default FavoritesPanel;
