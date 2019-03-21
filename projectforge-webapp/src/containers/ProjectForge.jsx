import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Router, Route, Switch } from 'react-router-dom';
import { loadUserStatus, loginUser, logoutUser } from '../actions';
import LoginView from '../components/authentication/LoginView';
import Footer from '../components/base/footer';
import Navigation from '../components/base/navigation';
import TopBar from '../components/base/topbar';
import { Container } from '../components/design';
import history from '../utilities/history';
import EditPage from './page/edit';
import ListPage from './page/list';

class ProjectForge extends React.Component {
    componentDidMount() {
        const { loadUserStatus: checkAuthentication } = this.props;

        checkAuthentication();
    }

    render() {
        const {
            user,
            loginUser: login,
            logoutUser: logout,
            loginInProgress,
            loginError,
            version,
        } = this.props;
        let content;

        if (user) {
            content = (
                <Router history={history}>
                    <React.Fragment>
                        <Navigation
                            logout={logout}
                            username={user.fullname}
                            // TODO: REMOVE EXAMPLE CATEGORIES
                            entries={[
                                {
                                    name: 'Bücher',
                                    url: '/books/',
                                },
                                {
                                    name: 'Buch bearbeiten',
                                    url: '/books/edit',
                                },
                                {
                                    name: 'Adressbücher',
                                    url: '/addresses/',
                                },
                            ]}
                            categories={[
                                {
                                    name: 'Allgemein',
                                    items: [
                                        {
                                            name: 'Kalender',
                                            url: '/',
                                        },
                                        {
                                            name: 'Kalenderliste',
                                            url: '/',
                                        },
                                        {
                                            name: 'Urlaubsantrag',
                                            url: '/',
                                        },
                                        {
                                            name: 'Bücher',
                                            url: '/books/',
                                        },
                                        {
                                            name: 'Adressbücher',
                                            url: '/addresses/',
                                        },
                                        {
                                            name: 'Adressen',
                                            url: '/',
                                        },
                                        {
                                            name: 'Direktwahl',
                                            url: '/',
                                        },
                                        {
                                            name: 'SMS senden',
                                            url: '/',
                                        },
                                        {
                                            name: 'Suche',
                                            url: '/',
                                        },
                                    ],
                                },
                                {
                                    name: 'Projektmanagement',
                                    items: [
                                        {
                                            name: 'Strukturbaum',
                                            url: '/',
                                        },
                                        {
                                            name: 'Zeitberichte',
                                            url: '/',
                                        },
                                        {
                                            name: 'Monatsbericht',
                                            url: '/',
                                        },
                                        {
                                            name: 'Meine Statistiken',
                                            url: '/',
                                        },
                                        {
                                            name: 'Personalplanung',
                                            url: '/',
                                        },
                                        {
                                            name: 'Personalplanungsliste',
                                            url: '/',
                                        },
                                        {
                                            name: 'Gantt',
                                            url: '/',
                                        },
                                    ],
                                },
                                {
                                    name: 'FiBu',
                                    items: [
                                        {
                                            name: 'Debitorenrechnungen',
                                            url: '/',
                                        },
                                        {
                                            name: 'Kreditorenrechnungen',
                                            url: '/',
                                        },
                                        {
                                            name: 'Kunden',
                                            url: '/',
                                        },
                                        {
                                            name: 'Projekte',
                                            url: '/',
                                        },
                                        {
                                            name: 'Auftragsbuch',
                                            url: '/',
                                        },
                                    ],
                                },
                                {
                                    name: 'Kost',
                                    items: [
                                        {
                                            name: 'Kost1',
                                            url: '/',
                                        },
                                        {
                                            name: 'Kost2',
                                            url: '/',
                                        },
                                        {
                                            name: 'Kost2-Arten',
                                            url: '/',
                                        },
                                    ],
                                },
                                {
                                    name: 'Reporting',
                                    items: [
                                        {
                                            name: 'Scriptliste',
                                            url: '/',
                                        },
                                        {
                                            name: 'Scripting',
                                            url: '/',
                                        },
                                        {
                                            name: 'Report-Objectives',
                                            url: '/',
                                        },
                                        {
                                            name: 'Buchungssätze',
                                            url: '/',
                                        },
                                        {
                                            name: 'Datev-Import',
                                            url: '/',
                                        },
                                    ],
                                },
                                {
                                    name: 'Organisation',
                                    items: [
                                        {
                                            name: 'Postausgang',
                                            url: '/',
                                        },
                                        {
                                            name: 'Posteingang',
                                            url: '/',
                                        },
                                    ],
                                },
                                {
                                    name: 'Administration',
                                    items: [
                                        {
                                            name: 'Mein Zugang',
                                            url: '/',
                                        },
                                        {
                                            name: 'Urlaubskonto',
                                            url: '/',
                                        },
                                        {
                                            name: 'Meine Einstellungen',
                                            url: '/',
                                        },
                                        {
                                            name: 'Passwort ändern',
                                            url: '/',
                                        },
                                        {
                                            name: 'Benutzer',
                                            url: '/',
                                        },
                                        {
                                            name: 'Gruppen',
                                            url: '/',
                                        },
                                        {
                                            name: 'Zugriffsverwaltung',
                                            url: '/',
                                        },
                                        {
                                            name: 'System',
                                            url: '/',
                                        },
                                        {
                                            name: 'Systemupdate',
                                            url: '/',
                                        },
                                        {
                                            name: 'Systemstatistik',
                                            url: '/',
                                        },
                                        {
                                            name: 'Konfiguration',
                                            url: '/',
                                        },
                                        {
                                            name: 'Plugins',
                                            url: '/',
                                        },
                                    ],
                                },
                            ]}
                        />
                        <Container fluid>
                            <Switch>
                                <Route
                                    path="/:category/edit/:id?"
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
                    motd="Please try user demo with password demo123. Have a lot of fun!"
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
                <Footer version={version} />
            </React.Fragment>
        );
    }
}

ProjectForge.propTypes = {
    loginUser: PropTypes.func.isRequired,
    logoutUser: PropTypes.func.isRequired,
    loadUserStatus: PropTypes.func.isRequired,
    loginInProgress: PropTypes.bool.isRequired,
    loginError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    user: PropTypes.shape({}),
    version: PropTypes.string,
};

ProjectForge.defaultProps = {
    loginError: undefined,
    user: undefined,
    version: 'Version unknown',
};

const mapStateToProps = state => ({
    loginInProgress: state.authentication.loading,
    loginError: state.authentication.error,
    user: state.authentication.user,
    version: state.authentication.version,
});

const actions = {
    loginUser,
    loadUserStatus,
    logoutUser,
};

export default connect(mapStateToProps, actions)(ProjectForge);
