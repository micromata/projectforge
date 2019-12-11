import { faChevronRight, faSearch } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import AwesomeDebouncePromise from 'awesome-debounce-promise';
import React from 'react';
import { Navbar } from 'reactstrap';
import { DynamicLayoutContext } from '../../../../components/base/dynamicLayout/context';
import Navigation from '../../../../components/base/navigation';
import { Col, Input, Row } from '../../../../components/design';
import AdvancedPopper from '../../../../components/design/popper/AdvancedPopper';
import AdvancedPopperAction from '../../../../components/design/popper/AdvancedPopperAction';
import { getNamedContainer } from '../../../../utilities/layout';
import { debouncedWaitTime, getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import FavoritesPanel from '../../../panel/favorite/FavoritesPanel';
import styles from '../ListPage.module.scss';
import { ListPageContext } from '../ListPageContext';
import MagicFilterPill from './MagicFilterPill';

const loadQuickSelectionsBounced = (
    {
        url,
        searchString = '',
        setQuickSelections,
    },
) => {
    fetch(
        getServiceURL(url.replace(':searchString', encodeURIComponent(searchString))),
        {
            method: 'GET',
            credentials: 'include',
            headers: { Accept: 'application/json' },
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(setQuickSelections);
};

function SearchFilter() {
    const {
        ui,
        setData,
    } = React.useContext(DynamicLayoutContext);

    const {
        category,
        filter,
        filterFavorites,
        filterHelper,
        openEditPage,
        quickSelectUrl,
        setFilterFavorites,
        setUI,
    } = React.useContext(ListPageContext);

    const [quickSelections, setQuickSelections] = React.useState([]);
    const [searchActive, setSearchActive] = React.useState(false);
    const [loadQuickSelections] = React.useState(
        () => AwesomeDebouncePromise(loadQuickSelectionsBounced, debouncedWaitTime),
    );

    const searchFilter = getNamedContainer('searchFilter', ui.namedContainers);

    // Initial QuickSelections call. Recall when url changed.
    React.useEffect(() => {
        if (quickSelectUrl) {
            loadQuickSelections({
                url: quickSelectUrl,
                searchString: filter.searchString,
                setQuickSelections,
            });
        }
    }, [quickSelectUrl, filter.searchString]);

    const saveUpdateResponse = (
        {
            data: responseData,
            ui: responseUI,
            filter: responseFilter,
            filterFavorites: responseFilterFavorites,
        },
    ) => {
        if (responseData) {
            setData(responseData);
        }
        if (responseUI) {
            setUI(responseUI);
        }
        if (responseFilter) {
            filterHelper.setFilterState(responseFilter);
        }
        if (responseFilterFavorites) {
            setFilterFavorites(responseFilterFavorites);
        }
    };

    const fetchFavorites = (action, { params = {}, body }) => fetch(
        getServiceURL(`${category}/filter/${action}`, params),
        {
            method: body ? 'POST' : 'GET',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
            },
            body: JSON.stringify(body),
        },
    )
        .then(handleHTTPErrors)
        .then(response => response.json())
        .then(saveUpdateResponse)
        .catch(error => alert(`Internal error: ${error}`));

    const handleFavoriteCreate = (newFilterName) => {
        filter.name = newFilterName;
        fetchFavorites('create', { body: filter });
    };
    const handleFavoriteDelete = id => fetchFavorites('delete', { params: { id } });
    const handleFavoriteSelect = id => fetchFavorites('select', { params: { id } });
    const handleFavoriteRename = (id, newName) => fetchFavorites('rename', {
        params: {
            id,
            newName,
        },
    });
    const handleFavoriteUpdate = () => fetchFavorites('update', { body: filter });

    const handleSearchStringChange = ({ target }) => filterHelper.setSearchString(target.value);

    return (
        <React.Fragment>
            <Row>
                <Col sm={4}>
                    <AdvancedPopper
                        additionalClassName={styles.completions}
                        setIsOpen={setSearchActive}
                        isOpen={searchActive}
                        basic={(
                            <Input
                                id="searchString"
                                icon={faSearch}
                                className={styles.search}
                                autoComplete="off"
                                placeholder={ui.translations.search}
                                onChange={handleSearchStringChange}
                                value={filter.searchString || ''}
                            />
                        )}
                        className={styles.searchContainer}
                        actions={(
                            <AdvancedPopperAction
                                type="delete"
                                disabled={!filter.searchString}
                                onClick={() => filterHelper.setSearchString('')}
                            >
                                {ui.translations.delete || ''}
                            </AdvancedPopperAction>
                        )}
                    >
                        <ul className={styles.entries}>
                            {/* TODO ADD KEYBOARD LISTENER FOR SELECTING */}
                            {/* TODO onClick Handler */}
                            {quickSelections.map(({ id, displayName }) => (
                                <li
                                    key={`quick-selection-${id}`}
                                    className={styles.entry}
                                    onClick={() => openEditPage(id)}
                                    role="option"
                                    aria-selected="false"
                                    onKeyPress={undefined}
                                >
                                    {displayName}
                                    <FontAwesomeIcon
                                        icon={faChevronRight}
                                        className={styles.icon}
                                    />
                                </li>
                            ))}
                        </ul>
                        {quickSelections.length === 0 && (
                            <p className={styles.errorMessage}>???No quick selections found.???</p>
                        )}
                    </AdvancedPopper>
                </Col>
                <Col sm={1} className="d-flex align-items-center">
                    <FavoritesPanel
                        onFavoriteCreate={handleFavoriteCreate}
                        onFavoriteDelete={handleFavoriteDelete}
                        onFavoriteRename={handleFavoriteRename}
                        onFavoriteSelect={handleFavoriteSelect}
                        onFavoriteUpdate={handleFavoriteUpdate}
                        favorites={filterFavorites}
                        currentFavoriteId={filter.id}
                        isModified
                        closeOnSelect={false}
                        translations={ui.translations}
                        htmlId="searchFilterFavoritesPopover"
                    />
                </Col>
                {/* Render the menu if it's loaded. */}
                {ui && ui.pageMenu && (
                    <Col>
                        <Navbar>
                            <Navigation
                                entries={ui.pageMenu}
                                // Let the menu float to the right.
                                className="ml-auto"
                            />
                        </Navbar>
                    </Col>
                )}
            </Row>
            <hr />
            <div className={styles.magicFilters}>
                {searchFilter && filter.entries
                    .map(({ field, value }) => ({
                        details: Array.findByField(searchFilter.content, 'id', field),
                        field,
                        value,
                    }))
                    .filter(({ details }) => details !== undefined)
                    .map(({ details }) => (
                        <MagicFilterPill
                            key={`magic-filter-${details.id}`}
                            translations={ui.translations}
                            name={details.label}
                            value="abc"
                        >
                            {details.label}
                        </MagicFilterPill>
                    ))}
                <MagicFilterPill
                    name="Firma"
                    value="Micromata"
                    translations={ui.translations}
                >
                    Input Firma
                </MagicFilterPill>
                <MagicFilterPill
                    name="Name"
                    translations={ui.translations}
                >
                    Input Name
                </MagicFilterPill>
                <MagicFilterPill
                    name="???Weitere Filter???"
                    translations={ui.translations}
                >
                    Weitere Filter
                </MagicFilterPill>
            </div>
            <hr />
            {/* TODO IMPLEMENT DIFFERENT SELECTION TYPES */}
        </React.Fragment>
    );
}

SearchFilter.propTypes = {};

SearchFilter.defaultProps = {};

export default SearchFilter;
