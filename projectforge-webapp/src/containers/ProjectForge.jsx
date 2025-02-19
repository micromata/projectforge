import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Routes } from 'react-router';
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
import ModalRoutes from './ModalRoutes';

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
    }, []);

    React.useEffect(() => {
        fetch(getServiceURL('/rsPublic/systemStatus'), { credentials: 'include' })
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => {
                const { setupRedirectUrl } = json;
                setSystemStatus(json);
                if (setupRedirectUrl) {
                    history.push(setupRedirectUrl);
                }
            });
    }, [user]);

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
        const getRoutesWithLocation = (switchLocation) => (
            <Routes location={switchLocation}>
                {wicketRoute}
                {publicRoute}
                <Route
                    path={`${prefix}/:tab?`}
                    element={<FormPage isPublic />}
                />
            </Routes>
        );

        content = <ModalRoutes getRoutesWithLocation={getRoutesWithLocation} />;
    }

    return (
        <SystemStatusContext.Provider
            /* eslint-disable-next-line react/jsx-no-constructed-context-values */
            value={{
                ...systemStatusContextDefaultValues,
                ...systemStatus,
            }}
        >
            <TopBar />
            {content}
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

const mapStateToProps = (state) => ({
    loginInProgress: state.authentication.loading,
    user: state.authentication.user,
});

const actions = {
    loadUserStatus,
};

export default connect(mapStateToProps, actions)(ProjectForge);
