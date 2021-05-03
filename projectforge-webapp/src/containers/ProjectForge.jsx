import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Router, Switch } from 'react-router-dom';
import { loadUserStatus } from '../actions';
import Footer from '../components/base/footer';
import Toasts from '../components/base/Toasts';
import TopBar from '../components/base/topbar';
import LoadingContainer from '../components/design/loading-container';
import history from '../utilities/history';
import prefix from '../utilities/prefix';
import { getServiceURL, handleHTTPErrors } from '../utilities/rest';
import AuthorizedRoutes, { publicRoute, wicketRoute } from './AuthorizedRoutes';
import FormPage from './page/form/FormPage';
import { SystemStatusContext, systemStatusContextDefaultValues } from './SystemStatusContext';

function ProjectForge(
    {
        user,
        loginInProgress,
        loadUserStatus: checkAuthentication,
    },
) {
    const [systemStatus, setSystemStatus] = React.useState({});

    React.useEffect(() => {
        checkAuthentication();

        fetch(getServiceURL('/rsPublic/systemStatus'))
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => {
                const { setupRedirectUrl } = json;
                setSystemStatus(json);
                if (setupRedirectUrl) {
                    history.push(setupRedirectUrl);
                }
            });
    }, []);

    let content;

    if (user) {
        content = <AuthorizedRoutes />;
    } else if (loginInProgress) {
        content = (
            <LoadingContainer loading>
                <h1>Logging in...</h1>
            </LoadingContainer>
        );
    } else {
        content = (
            <Switch>
                {wicketRoute}
                {publicRoute}
                <Route
                    path={prefix}
                    render={({ match, location, ...props }) => (
                        <FormPage
                            // eslint-disable-next-line react/jsx-props-no-spreading
                            {...props}
                            location={location}
                            isPublic
                            match={{
                                ...match,
                                // Disable FormPage Tabs
                                url: location.pathname,
                                // Set Category to login
                                params: {
                                    category: 'login',
                                },
                            }}
                        />
                    )}
                />
            </Switch>
        );
    }

    return (
        <SystemStatusContext.Provider
            value={{
                ...systemStatusContextDefaultValues,
                ...systemStatus,
            }}
        >
            <TopBar />
            <Router history={history}>
                {content}
            </Router>
            <Footer />
            <Toasts />
        </SystemStatusContext.Provider>
    );
}

ProjectForge.propTypes = {
    loadUserStatus: PropTypes.func.isRequired,
    loginInProgress: PropTypes.bool.isRequired,
    user: PropTypes.shape({}),
};

ProjectForge.defaultProps = {
    user: undefined,
};

const mapStateToProps = (state) => ({
    loginInProgress: state.authentication.loading,
    user: state.authentication.user,
});

const actions = {
    loadUserStatus,
};

export default connect(mapStateToProps, actions)(ProjectForge);
