import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { BrowserRouter as Router } from 'react-router-dom';
import TopBar from '../components/base/topbar';
import Footer from '../components/base/footer';
import LoginView from '../components/authentication/LoginView';
import { loadSessionIfAvailable, loginUser, logoutUser } from '../actions';
import Navigation from '../components/base/navigation';
import style from './ProjectForge.module.scss';
import Page from '../components/base/page';

class ProjectForge extends Component {
    componentDidMount() {
        const { loadSessionIfAvailable: loadSession } = this.props;

        loadSession();
    }


    render() {
        const {
            loggedIn,
            loginUser: login,
            logoutUser: logout,
            loginInProgress,
            loginError,
        } = this.props;
        let content;

        if (loggedIn) {
            content = (
                <Router>
                    <div className={style.content}>
                        <Navigation
                            logout={logout}
                            // TODO: REMOVE EXAMPLE CATEGORIES
                            username="Demo User"
                            entries={[
                                {
                                    name: 'Administration',
                                    items: [
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
                                    ],
                                },
                                {
                                    name: 'Passwort ändern',
                                    url: '/',
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
                                            url: '/',
                                        },
                                        {
                                            name: 'Adressbücher',
                                            url: '/',
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
                        <Page />
                    </div>
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
                <Footer version="Version 6.25-SNAPSHOT, 2019-02-26" updateAvailable />
            </React.Fragment>
        );
    }
}

ProjectForge.propTypes = {
    loginUser: PropTypes.func.isRequired,
    loggedIn: PropTypes.bool.isRequired,
    logoutUser: PropTypes.func.isRequired,
    loadSessionIfAvailable: PropTypes.func.isRequired,
    loginInProgress: PropTypes.bool.isRequired,
    loginError: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
};

ProjectForge.defaultProps = {
    loginError: undefined,
};

const mapStateToProps = state => ({
    loggedIn: state.authentication.user !== null,
    loginInProgress: state.authentication.loading,
    loginError: state.authentication.error,
});

const actions = {
    loginUser,
    loadSessionIfAvailable,
    logoutUser,
};

export default connect(mapStateToProps, actions)(ProjectForge);
