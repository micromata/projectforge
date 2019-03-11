import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import TopBar from '../components/base/topbar';
import Footer from '../components/base/footer';
import LoginView from '../components/authentication/LoginView';
import { loadSessionIfAvailable, loginUser } from '../actions';
import Navigation from '../components/base/navigation';
import style from './ProjectForge.module.scss';

class ProjectForge extends Component {
    componentDidMount() {
        const { loadSessionIfAvailable: loadSession } = this.props;

        loadSession();
    }


    render() {
        const {
            loggedIn,
            loginUser: login,
            loginInProgress,
            loginError,
        } = this.props;
        let content;

        if (loggedIn) {
            content = (
                <div className={style.content}>
                    <Navigation
                        // TODO: REMOVE EXAMPLE CATEGORIES
                        categories={[
                            {
                                name: 'Allgemein',
                                items: [
                                    { name: 'Kalender' },
                                    { name: 'Kalenderliste' },
                                    { name: 'Urlaubsantrag' },
                                    { name: 'Bücher' },
                                    { name: 'Adressbücher' },
                                    { name: 'Adressen' },
                                    { name: 'Direktwahl' },
                                    { name: 'SMS senden' },
                                    { name: 'Suche' },
                                ],
                            },
                            {
                                name: 'Projektmanagement',
                                items: [
                                    { name: 'Strukturbaum' },
                                    { name: 'Zeitberichte' },
                                    { name: 'Monatsbericht' },
                                    { name: 'Meine Statistiken' },
                                    { name: 'Personalplanung' },
                                    { name: 'Personalplanungsliste' },
                                    { name: 'Gantt' },
                                ],
                            },
                            {
                                name: 'FiBu',
                                items: [
                                    { name: 'Debitorenrechnungen' },
                                    { name: 'Kreditorenrechnungen' },
                                    { name: 'Kunden' },
                                    { name: 'Projekte' },
                                    { name: 'Auftragsbuch' },
                                ],
                            },
                            {
                                name: 'Kost',
                                items: [
                                    { name: 'Kost1' },
                                    { name: 'Kost2' },
                                    { name: 'Kost2-Arten' },
                                ],
                            },
                            {
                                name: 'Reporting',
                                items: [
                                    { name: 'Scriptliste' },
                                    { name: 'Scripting' },
                                    { name: 'Report-Objectives' },
                                    { name: 'Buchungssätze' },
                                    { name: 'Datev-Import' },
                                ],
                            },
                            {
                                name: 'Organisation',
                                items: [
                                    { name: 'Postausgang' },
                                    { name: 'Posteingang' },
                                ],
                            },
                            {
                                name: 'Administration',
                                items: [
                                    { name: 'Mein Zugang' },
                                    { name: 'Urlaubskonto' },
                                    { name: 'Meine Einstellungen' },
                                    { name: 'Passwort ändern' },
                                    { name: 'Benutzer' },
                                    { name: 'Gruppen' },
                                    { name: 'Zugriffsverwaltung' },
                                    { name: 'System' },
                                    { name: 'Systemupdate' },
                                    { name: 'Systemstatistik' },
                                    { name: 'Konfiguration' },
                                    { name: 'Plugins' },
                                ],
                            },
                        ]}
                    />
                </div>
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
};

export default connect(mapStateToProps, actions)(ProjectForge);
