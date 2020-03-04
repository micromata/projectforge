import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Router, Switch } from 'react-router-dom';
import { loadUserStatus, loginUser } from '../actions';
import LoginView from '../components/authentication/LoginView';
import Footer from '../components/base/footer';
import TopBar from '../components/base/topbar';
import history from '../utilities/history';
import prefix from '../utilities/prefix';
import { getServiceURL, handleHTTPErrors } from '../utilities/rest';
import AuthorizedRoutes, { wicketRoute } from './AuthorizedRoutes';
import DynamicPage from './page/DynamicPage';
import FormPage from './page/form/FormPage';
import { SystemStatusContext, systemStatusContextDefaultValues } from './SystemStatusContext';

function ProjectForge(
    {
        user,
        loginUser: login,
        loginInProgress,
        loginError,
        loadUserStatus: checkAuthentication,
    },
) {
    const [systemStatus, setSystemStatus] = React.useState({});

    React.useEffect(() => {
        checkAuthentication();

        fetch(getServiceURL('../rsPublic/systemStatus'))
            .then(handleHTTPErrors)
            .then(response => response.json())
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
    } else {
        content = (
            <Switch>
                {wicketRoute}
                <Route
                    path={`${prefix}:restPrefix/:category/:type?`}
                    component={FormPage}
                />
                <Route
                    path={prefix}
                    render={props => (
                        <LoginView
                            // TODO REPLACE OLD LOGIN VIEW WITH DYNAMIC PAGE
                            {...props}
                            motd="[Please try user demo with password demo123. Have a lot of fun!]"
                            login={login}
                            loading={loginInProgress}
                            error={loginError}
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
        </SystemStatusContext.Provider>
    );
}

ProjectForge.propTypes = {
    loginUser: PropTypes.func.isRequired,
    loadUserStatus: PropTypes.func.isRequired,
    loginInProgress: PropTypes.bool.isRequired,
    loginError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    user: PropTypes.shape({}),
};

ProjectForge.defaultProps = {
    loginError: undefined,
    user: undefined,
};

const mapStateToProps = state => ({
    loginInProgress: state.authentication.loading,
    loginError: state.authentication.error,
    user: state.authentication.user,
});

const actions = {
    loginUser,
    loadUserStatus,
};

export default connect(mapStateToProps, actions)(ProjectForge);
