import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Router, Switch } from 'react-router-dom';
import { loadUserStatus, loginUser } from '../actions';
import LoginView from '../components/authentication/LoginView';
import Footer from '../components/base/footer';
import GlobalNavigation from '../components/base/navigation/GlobalNavigation';
import TopBar from '../components/base/topbar';
import { Alert, Container } from '../components/design';
import history from '../utilities/history';
import prefix from '../utilities/prefix';
import { getServiceURL, handleHTTPErrors } from '../utilities/rest';
import CalendarPage from './page/calendar/CalendarPage';
import DynamicPage from './page/DynamicPage';
import EditPage from './page/edit/EditPage';
import IndexPage from './page/IndexPage';
import ListPage from './page/list/ListPage';
import TaskTreePage from './page/TaskTreePage';
import { SystemStatusContext, systemStatusContextDefaultValues } from './SystemStatusContext';

function ProjectForge(
    {
        alertMessage,
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

    const wicketRoute = (
        <Route
            path="/wa"
            component={({ location }) => {
                if (process.env.NODE_ENV === 'development') {
                    return (
                        <a href={getServiceURL(`..${location.pathname}`)}>
                            Redirect to Wicket
                        </a>
                    );
                }

                window.location.reload();
                return <React.Fragment />;
            }}
        />
    );
    let content;

    if (user) {
        content = (
            <React.Fragment>
                <GlobalNavigation />
                <Container fluid>
                    {alertMessage ? (
                        <Alert color="danger">
                            {alertMessage}
                        </Alert>
                    ) : undefined}
                    <Switch>
                        {wicketRoute}
                        <Route
                            exact
                            path={prefix}
                            component={IndexPage}
                        />
                        <Route
                            path={`${prefix}calendar`}
                            component={CalendarPage}
                        />
                        <Route
                            path={`${prefix}taskTree`}
                            component={TaskTreePage}
                        />
                        <Route
                            path={`${prefix}dynamic/:page`}
                            component={DynamicPage}
                        />
                        <Route
                            path={`${prefix}:category/edit/:id?`}
                            component={EditPage}
                        />
                        <Route
                            path={`${prefix}:category`}
                            component={ListPage}
                        />
                    </Switch>
                </Container>
            </React.Fragment>
        );
    } else {
        content = (
            <Switch>
                {wicketRoute}
                <Route
                    path={`${prefix}:restPrefix/:page`}
                    component={DynamicPage}
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
    alertMessage: PropTypes.string,
    loginError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    user: PropTypes.shape({}),
};

ProjectForge.defaultProps = {
    alertMessage: undefined,
    loginError: undefined,
    user: undefined,
};

const mapStateToProps = state => ({
    loginInProgress: state.authentication.loading,
    loginError: state.authentication.error,
    user: state.authentication.user,
    alertMessage: state.authentication.alertMessage,
});

const actions = {
    loginUser,
    loadUserStatus,
};

export default connect(mapStateToProps, actions)(ProjectForge);
