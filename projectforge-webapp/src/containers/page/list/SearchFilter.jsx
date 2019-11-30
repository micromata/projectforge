import { faSearch } from '@fortawesome/free-solid-svg-icons';
import React from 'react';
import { Navbar } from 'reactstrap';
import { DynamicLayoutContext } from '../../../components/base/dynamicLayout/context';
import Navigation from '../../../components/base/navigation';
import { Col, Input, Row } from '../../../components/design';
import { getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import FavoritesPanel from '../../panel/favorite/FavoritesPanel';
import styles from './ListPage.module.scss';
import { ListPageContext } from './ListPageContext';


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
        setFilterFavorites,
        setUI,
    } = React.useContext(ListPageContext);

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

    return (
        <React.Fragment>
            <Row>
                <Col sm={4}>
                    <Input
                        id="searchFilter"
                        icon={faSearch}
                        className={styles.search}
                        autoComplete="off"
                        placeholder={ui.translations.search}
                        // TODO ADD DELETE BUTTON
                    />
                    {/* TODO ADD AUTO COMPLETION */}
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
            {/* TODO ADD MAGIC FILTERS */}
        </React.Fragment>
    );
}

SearchFilter.propTypes = {};

SearchFilter.defaultProps = {};

export default SearchFilter;
