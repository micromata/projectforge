import PropTypes from 'prop-types';
import React, { useLayoutEffect } from 'react';
import { Popover, PopoverBody } from '../../../components/design';
import style from '../../../components/design/input/Input.module.scss';
import { useClickOutsideHandler } from '../../../utilities/hooks';
import FavoriteEntry from './FavoriteEntry';
import FavoriteNameInput from './FavoriteNameInput';
import FavoritesButton from './FavoritesButton';

function FavoritesPanel(
    {
        closeOnSelect = true,
        currentFavoriteId = -1,
        htmlId = 'favoritesPopover',
        isModified = false,
        favorites = [],
        favoriteButtonText,
        favoriteButtonTooltip,
        onFavoriteCreate,
        onFavoriteDelete,
        onFavoriteRename,
        onFavoriteSelect,
        onFavoriteUpdate,
        newFavoriteI18nKey = 'favorite.addNew',
        newFavoriteTooltipI18nKey,
        translations = {},
    },
) {
    const [open, setOpen] = React.useState(false);
    const popperRef = React.useRef(null);
    const [maxListHeight, setMaxListHeight] = React.useState('300px');

    const toggle = () => {
        // event.nativeEvent?.stopPropagation();
        setOpen(!open);
    };

    const handleFavoriteSelect = (id, name) => {
        if (closeOnSelect) {
            setOpen(false);
        }
        if (onFavoriteSelect) {
            onFavoriteSelect(id, name);
        }
    };

    useClickOutsideHandler(popperRef, setOpen, open);

    useLayoutEffect(() => {
        if (popperRef.current) {
            setMaxListHeight(`calc(100vh - ${popperRef.current.getBoundingClientRect().top}px - 10rem)`);
        }
    }, [open]);

    return (
        <>
            <FavoritesButton
                toggle={toggle}
                id={htmlId}
                isOpen={open}
                favoriteButtonText={favoriteButtonText}
                favoriteButtonTooltip={favoriteButtonTooltip}
                translations={translations}
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
                    <div
                        ref={popperRef}
                        style={{
                            maxHeight: maxListHeight,
                            overflow: 'scroll',
                        }}
                    >
                        {onFavoriteCreate && (
                            <FavoriteNameInput
                                className={style.favoritesName}
                                defaultValue={translations['favorite.untitled'] || 'untitled'}
                                id="newFilterName"
                                onSave={onFavoriteCreate}
                                label={translations[newFavoriteI18nKey] || 'Add new'}
                                tooltip={translations[newFavoriteTooltipI18nKey]}
                            />
                        )}
                        <ul className={style.favoritesList}>
                            {favorites.map((favorite) => (
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
                    </div>
                </PopoverBody>
            </Popover>
        </>
    );
}

FavoritesPanel.propTypes = {
    onFavoriteSelect: PropTypes.func.isRequired,
    // Should the pop-over be closed after a favorite entry was selected?
    closeOnSelect: PropTypes.bool,
    // The current used favorite (can be saved with modified settings).
    currentFavoriteId: PropTypes.number,
    favoriteButtonText: PropTypes.string,
    favoriteButtonTooltip: PropTypes.string,
    favorites: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number,
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
    newFavoriteI18nKey: PropTypes.string,
    newFavoriteTooltipI18nKey: PropTypes.string,
    translations: PropTypes.shape({
        'favorite.untitled': PropTypes.string,
    }),
};

export default FavoritesPanel;
