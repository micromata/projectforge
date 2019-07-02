import PropTypes from 'prop-types';
import React from 'react';
import DynamicLayout from '../../../components/base/dynamicLayout';
import { registerComponent } from '../../../components/base/dynamicLayout/components/DynamicRenderer';
import DynamicCheckbox
    from '../../../components/base/dynamicLayout/components/input/DynamicCheckbox';
import LoadingContainer from '../../../components/design/loading-container';
import { getObjectFromQuery, getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import DynamicFilterCheckbox from './DynamicFilterCheckbox';
import SearchFilter from './SearchFilter';

function ListPage(
    {
        match,
        location,
    },
) {
    const [ui, setUI] = React.useState({ translations: {} });
    const [data, setData] = React.useState({});
    const [filter, setFilter] = React.useState({});
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(undefined);

    // Register DynamicFilterCheckbox only for the ListPage
    // Attention: Can't use DynamicLayout twice here. Because the normal checkbox got an override.
    React.useEffect(() => {
        registerComponent('CHECKBOX', DynamicFilterCheckbox);

        return () => registerComponent('CHECKBOX', DynamicCheckbox);
    }, []);

    const loadList = () => {
        setLoading(true);
        setError(undefined);
        fetch(
            getServiceURL(
                `${match.params.category}/initialList`,
                getObjectFromQuery(location.search || ''),
            ),
            {
                method: 'GET',
                credentials: 'include',
            },
        )
            .then((response) => {
                setLoading(false);
                return response;
            })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(({ ui: responseUi, data: responseData, filter: responseFilter }) => {
                setUI(responseUi);
                setData(responseData);
                setFilter(responseFilter);
            })
            .catch(responseError => setError(responseError));
    };

    // Only reload the list when the category or search string changes.
    React.useEffect(loadList, [match.params.category, location.search]);

    if (error) {
        return <h4>{error.message}</h4>;
    }

    const dynamicSetFilter = newFilter => setFilter(stateFilter => ({
        ...stateFilter,
        ...newFilter,
    }));

    return (
        <LoadingContainer loading={loading}>
            <DynamicLayout
                ui={ui}
                data={data}
                options={{
                    displayPageMenu: true,
                    setBrowserTitle: false,
                    showActionButtons: false,
                }}
                filter={filter}
                setFilter={dynamicSetFilter}
                category={match.params.category}
            >
                <SearchFilter />
            </DynamicLayout>
        </LoadingContainer>
    );
}

ListPage.propTypes = {
    location: PropTypes.shape({
        search: PropTypes.string,
    }).isRequired,
    match: PropTypes.shape({
        params: PropTypes.shape({
            category: PropTypes.string.isRequired,
        }).isRequired,
    }).isRequired,
};

ListPage.defaultProps = {};

export default ListPage;
