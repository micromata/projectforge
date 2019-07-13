import PropTypes from 'prop-types';
import React from 'react';
import DynamicLayout from '../../components/base/dynamicLayout';
import LoadingContainer from '../../components/design/loading-container';
import { getServiceURL, handleHTTPErrors } from '../../utilities/rest';

const getRestURL = (url, match) => getServiceURL(`${match && match.params.restPrefix === 'public' ? '../rsPublic/' : ''}${url}`);

function DynamicPage({ match }) {
    const [ui, setUI] = React.useState({});
    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(undefined);

    const loadLayout = () => {
        setLoading(true);
        setError(undefined);

        fetch(getRestURL(`${match.params.page}/layout`, match))
            .then((response) => {
                setLoading(false);
                return response;
            })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(setUI)
            .catch(setError);
    };

    React.useEffect(loadLayout, [match.params.page]);

    if (error) {
        return <h4>{error.message}</h4>;
    }

    return (
        <LoadingContainer loading={loading}>
            <DynamicLayout ui={ui} />
        </LoadingContainer>
    );
}

DynamicPage.propTypes = {
    match: PropTypes.shape({
        params: PropTypes.shape({
            page: PropTypes.string.isRequired,
            restPrefix: PropTypes.string,
        }).isRequired,
    }).isRequired,
};

DynamicPage.defaultProps = {};

export default DynamicPage;
