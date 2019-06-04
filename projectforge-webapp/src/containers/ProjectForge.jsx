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
import CalendarPage from './page/CalendarPage';
import EditPage from './page/edit';
import IndexPage from './page/IndexPage';
import InputTestPage from './page/InputTest';
import ListPage from './page/list';
import TaskTreePage from './page/TaskTreePage';

class ProjectForge extends React.Component {
    componentDidMount() {
        const { loadUserStatus: checkAuthentication } = this.props;

        checkAuthentication();
    }

    render() {
        const {
            user,
            loginUser: login,
            loginInProgress,
            loginError,
            version,
            releaseTimestamp,
        } = this.props;
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
            <React.Fragment>
                <TopBar />
                {content}
                <Footer version={version} releaseTimestamp={releaseTimestamp} />
            </React.Fragment>
        );
    }
}

ProjectForge.propTypes = {
    loginUser: PropTypes.func.isRequired,
    loadUserStatus: PropTypes.func.isRequired,
    loginInProgress: PropTypes.bool.isRequired,
    loginError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    user: PropTypes.shape({}),
    version: PropTypes.string,
    releaseTimestamp: PropTypes.string,
};

ProjectForge.defaultProps = {
    loginError: undefined,
    user: undefined,
    version: 'Version unknown',
    releaseTimestamp: '01.01.1970 08:00',
};

const mapStateToProps = state => ({
    loginInProgress: state.authentication.loading,
    loginError: state.authentication.error,
    user: state.authentication.user,
    version: state.authentication.version,
    releaseTimestamp: state.authentication.releaseTimestamp,
});

const actions = {
    loginUser,
    loadUserStatus,
};

export default connect(mapStateToProps, actions)(ProjectForge);
