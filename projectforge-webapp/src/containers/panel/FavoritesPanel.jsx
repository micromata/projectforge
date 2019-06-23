import { faStar } from '@fortawesome/free-regular-svg-icons';
import {
    faCheck,
    faCheckSquare,
    faEdit,
    faSync,
    faTrashAlt
} from '@fortawesome/free-solid-svg-icons';
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
            newFilterName: '',
            popoverOpen: false,
        };

        this.handleInputChange = this.handleInputChange.bind(this);
        this.onCreateClick = this.onCreateClick.bind(this);
        this.onDeleteClick = this.onDeleteClick.bind(this);
        this.onRenameClick = this.onRenameClick.bind(this);
        this.onSelectClick = this.onSelectClick.bind(this);
        this.onUpdateClick = this.onUpdateClick.bind(this);
        this.togglePopover = this.togglePopover.bind(this);
    }


    onCreateClick(event) {
        event.preventDefault();
        event.stopPropagation();
        const { newFilterName } = this.state;
        const { onFavoriteCreate } = this.props;
        onFavoriteCreate(newFilterName);
    }

    onDeleteClick(event, id) {
        event.preventDefault();
        event.stopPropagation();
        const { onFavoriteDelete } = this.props;
        onFavoriteDelete(id);
    }

    onRenameClick(event, id, newName) {
        event.preventDefault();
        event.stopPropagation();
        const { onFavoriteRename } = this.props;
        onFavoriteRename(id, newName);
    }

    onSelectClick(event, id) {
        event.preventDefault();
        event.stopPropagation();
        const { onFavoriteSelect } = this.props;
        onFavoriteSelect(id);
    }

    onUpdateClick(event, id) {
        event.preventDefault();
        event.stopPropagation();
        const { onFavoriteUpdate } = this.props;
        if (onFavoriteUpdate) {
            onFavoriteUpdate(id);
        }
    }

    handleInputChange(event) {
        const { id, value } = event.target;
        this.setState({
            [id]: value,
        });
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
            isModified,
            favorites,
            translations,
        } = this.props;

        const syncIcon = isModified
            ? (
                <React.Fragment>
                    <FontAwesomeIcon
                        id="syncFavorite"
                        onClick={
                            event => this.onUpdateClick(event, currentFavoriteId)
                        }
                        icon={faSync}
                        className={classNames(
                            style.icon,
                            style.syncIcon,
                        )}
                    />
                    <UncontrolledTooltip
                        placement="right"
                        target="syncFavorite"
                    >
                        {translations.save}
                    </UncontrolledTooltip>
                </React.Fragment>
            )
            : (
                <React.Fragment>
                    <FontAwesomeIcon
                        id="syncFavorite"
                        icon={faCheck}
                        className={classNames(
                            style.icon,
                            style.syncIcon,
                        )}
                    />
                    <UncontrolledTooltip
                        placement="right"
                        target="syncFavorite"
                    >
                        {translations.uptodate}
                    </UncontrolledTooltip>
                </React.Fragment>
            );
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
                                <Input
                                    id="newFilterName"
                                    label={translations['favorite.addNew']}
                                    onChange={this.handleInputChange}
                                />
                                <FontAwesomeIcon
                                    className={classNames(
                                        style.icon,
                                        style.saveIcon,
                                    )}
                                    icon={faCheckSquare}
                                    size="lg"
                                    onClick={event => this.onCreateClick(event)}
                                />
                            </li>
                            {favorites.map(favorite => (
                                <li
                                    key={favorite.id}
                                    onClick={event => this.onSelectClick(event, favorite.id)}
                                    role="presentation"
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
                                            onClick={event => this.onRenameClick(event, favorite.id)}
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
                                            onClick={event => this.onDeleteClick(event, favorite.id)}
                                        />
                                        <UncontrolledTooltip
                                            placement="right"
                                            target={`del-${favorite.id}`}
                                        >
                                            {translations.delete}
                                        </UncontrolledTooltip>
                                        {' '}
                                        {favorite.id === currentFavoriteId ? (
                                            syncIcon
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
    onFavoriteCreate: PropTypes.func.isRequired,
    onFavoriteDelete: PropTypes.func.isRequired,
    onFavoriteRename: PropTypes.func.isRequired,
    onFavoriteSelect: PropTypes.func.isRequired,
    onFavoriteUpdate: PropTypes.func,
    // Is true, if the current favorite filter is modified and is ready for update, otherwise false.
    // Default is false (so favorite can't be updated)
    isModified: PropTypes.bool,
    translations: PropTypes.shape({}), // .isRequired, TODO: SearchFilter has no translations!?
};

FavoritesPanel.defaultProps = {
    currentFavoriteId: 0,
    favorites: [],
    translations: [],
    isModified: false,
    onFavoriteUpdate: undefined,
};

export default (FavoritesPanel);
