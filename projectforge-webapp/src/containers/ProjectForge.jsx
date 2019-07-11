import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Router, Switch } from 'react-router-dom';
import { loadUserStatus, loginUser } from '../actions';
import LoginView from '../components/authentication/LoginView';
import Footer from '../components/base/footer';
import GlobalNavigation from '../components/base/navigation/GlobalNavigation';
import TopBar from '../components/base/topbar';
import { Container } from '../components/design';
import history from '../utilities/history';
import { getServiceURL, handleHTTPErrors } from '../utilities/rest';
import CalendarPage from './page/calendar/CalendarPage';
import EditPage from './page/edit/EditPage';
import IndexPage from './page/IndexPage';
import InputTestPage from './page/InputTest';
import ListPage from './page/list/ListPage';
import TaskTreePage from './page/TaskTreePage';
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

        fetch(
            getServiceURL('../rsPublic/systemStatus'),
            {
                method: 'GET',
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(setSystemStatus);
    }, []);

    let content;

    if (user) {
        content = (
            <Router history={history}>
                <React.Fragment>
                    <GlobalNavigation />
                    <Container fluid>
                        <Switch>
                            <Route
                                exact
                                path="/"
                                component={IndexPage}
                            />
                            <Route
                                path="/wa"
                                component={() => window.location.reload()}
                            />
                            <Route
                                path="/calendar"
                                component={CalendarPage}
                            />
                            <Route
                                path="/taskTree"
                                component={TaskTreePage}
                            />
                            <Route
                                path="/inputTest"
                                component={InputTestPage}
                            />
                            <Route
                                path="/:category/edit/:id?/:tab?"
                                component={EditPage}
                            />
                            <Route
                                path="/:category/"
                                component={ListPage}
                            />
                        </Switch>
                    </Container>
                </React.Fragment>
            </Router>
        );
    } else {
        content = (
            <LoginView
                // TODO: EXAMPLE DATA, REPLACE WITH REAL DATA FROM REST API
                motd="[Please try user demo with password demo123. Have a lot of fun!]"
                login={login}
                loading={loginInProgress}
                error={loginError}
            />
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
            {content}
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
