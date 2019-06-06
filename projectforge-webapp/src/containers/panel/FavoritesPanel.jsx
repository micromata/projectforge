import { faStar } from '@fortawesome/free-regular-svg-icons';
import { faCheckSquare, faEdit, faSync, faTrashAlt } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React, { Component } from 'react';
import { Button, Popover, PopoverBody, UncontrolledTooltip } from 'reactstrap';
import Input from '../../components/design/input';
import style from '../../components/design/input/Input.module.scss';

/**
 * Favorite filters and settings of the user.
 */
class FavoritesPanel extends Component {
    constructor(props) {
        super(props);
        this.state = {
            popoverOpen: false,
        };

        this.togglePopover = this.togglePopover.bind(this);
    }

    togglePopover() {
        this.setState(prevState => ({
            popoverOpen: !prevState.popoverOpen,
        }));
    }

    render() {
        const { popoverOpen } = this.state;
        const {
            currentFavoriteId,
            favorites,
            onFavoriteDelete,
            onFavoriteUpdate,
            translations,
        } = this.props;
        return (
            <React.Fragment>
                <Button
                    id="favoritesPopover"
                    color="link"
                    className="selectPanelIconLinks"
                    onClick={this.togglePopover}
                >
                    <FontAwesomeIcon
                        icon={faStar}
                        className={style.icon}
                        size="lg"
                    />
                </Button>
                <Popover
                    placement="left-start"
                    isOpen={popoverOpen}
                    target="favoritesPopover"
                    toggle={this.togglePopover}
                    trigger="legacy"
                >
                    <PopoverBody>
                        <ul className={style.favoritesList}>
                            <li className={style.addFavorite}>
                                <Input label="Add new Filter" id="add-new-filter" />
                                <FontAwesomeIcon
                                    className={classNames(
                                        style.icon,
                                        style.saveIcon,
                                    )}
                                    icon={faCheckSquare}
                                    size="lg"
                                />
                            </li>
                            {favorites.map(favorite => (
                                <li
                                    key={favorite.id}
                                    className={classNames(
                                        style.favorite,
                                        { [style.selected]: favorite.id === currentFavoriteId },
                                    )}
                                >
                                    <span className={style.favoriteName}>
                                        {favorite.name}
                                    </span>
                                    <div className={style.actions}>
                                        <FontAwesomeIcon
                                            id={`ren-${favorite.id}`}
                                            icon={faEdit}
                                            className={style.icon}
                                        />
                                        <UncontrolledTooltip
                                            placement="right"
                                            target={`ren-${favorite.id}`}
                                        >
                                            {translations.rename}
                                        </UncontrolledTooltip>
                                        <FontAwesomeIcon
                                            id={`del-${favorite.id}`}
                                            icon={faTrashAlt}
                                            className={classNames(
                                                style.icon,
                                                style.deleteIcon,
                                            )}
                                            onClick={() => onFavoriteDelete(favorite.id)}
                                        />
                                        <UncontrolledTooltip
                                            placement="right"
                                            target={`del-${favorite.id}`}
                                        >
                                            {translations.delete}
                                        </UncontrolledTooltip>
                                        {' '}
                                        {favorite.id === currentFavoriteId ? (
                                            <React.Fragment>
                                                <FontAwesomeIcon
                                                    id="syncFavorite"
                                                    onClick={
                                                        () => onFavoriteUpdate(favorite.id)
                                                    }
                                                    icon={faSync}
                                                    className={classNames(
                                                        style.icon,
                                                        style.syncIcon,
                                                    )}
                                                    color="grey"
                                                />
                                                <UncontrolledTooltip
                                                    placement="right"
                                                    target="syncFavorite"
                                                >
                                                    {translations.save}
                                                </UncontrolledTooltip>
                                            </React.Fragment>
                                        ) : ''}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </PopoverBody>
                </Popover>
            </React.Fragment>
        );
    }
}

FavoritesPanel.propTypes = {
    favorites: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number,
        name: PropTypes.string,
    })),
    // The current used favorite (can be saved with modified settings).
    currentFavoriteId: PropTypes.number,
    onFavoriteDelete: PropTypes.func.isRequired,
    onFavoriteRename: PropTypes.func.isRequired,
    onFavoriteSelect: PropTypes.func.isRequired,
    onFavoriteUpdate: PropTypes.func.isRequired,
    translations: PropTypes.shape({}), // .isRequired, TODO: SearchFilter has no translations!?
};

FavoritesPanel.defaultProps = {
    currentFavoriteId: 2,
    favorites: [{
        id: 1,
        name: 'My filter 1',
    }, {
        id: 2,
        name: 'Untitled 2',
    }, {
        id: 3,
        name: 'My lovely filter 3',
    }, {
        id: 4,
        name: 'My lovely, lovely, loverly Superfilter 4',
    }, {
        id: 5,
        name: 'Filter 5',
    }, {
        id: 6,
        name: 'Filter 6',
    }, {
        id: 7,
        name: 'Filter 7',
    }],
    translations: [],
};

export default (FavoritesPanel);
