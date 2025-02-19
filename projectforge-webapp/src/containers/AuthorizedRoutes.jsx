import Helmet from 'react-helmet';
import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route, Routes } from 'react-router';
import GlobalNavigation from '../components/base/navigation/GlobalNavigation';
import { Alert, Container } from '../components/design';
import prefix from '../utilities/prefix';
import CalendarPage from './page/calendar/CalendarPage';
import FormPage from './page/form/FormPage';
import IndexPage from './page/IndexPage';
import ListPage from './page/list/ListPage';
import TaskTreePage from './page/TaskTreePage';
import ModalRoutes from './ModalRoutes';
import RedirectToWicket from './RedirectToWicket';
import FormModal from './page/form/FormModal';

export const wicketRoute = (
    <Route
        path="/wa/*"
        element={<RedirectToWicket />}
    />
);

export const publicRoute = (
    <Route
        path={`${prefix}public/:category/:type?/:id?/:tab?`}
        element={<FormPage isPublic />}
    />
);

function AuthorizedRoutes(
    {
        alertMessage,
        locale = 'en',
    },
) {
    const getRoutesWithLocation = (location) => (
        <Routes location={location}>
            {wicketRoute}
            {publicRoute}
            <Route
                exact
                path={prefix}
                element={<IndexPage />}
            />
            <Route
                path={`${prefix}calendar`}
                element={<CalendarPage />}
            >
                <Route
                    path={`${prefix}calendar/:category/:type/:id?/:tab?`}
                    element={<FormModal />}
                />
            </Route>
            <Route
                path={`${prefix}taskTree`}
                element={<TaskTreePage />}
            />
            <Route
                path={`${prefix}:category/:type/:id?/:tab?`}
                element={<FormPage />}
            />
            <Route
                path={`${prefix}:category`}
                element={<ListPage />}
            />
        </Routes>
    );

    return (
        <>
            <Helmet>
                <html lang={locale} />
            </Helmet>
            <GlobalNavigation />
            {alertMessage ? (
                <Container fluid>
                    <Alert color="danger">
                        {alertMessage}
                    </Alert>
                </Container>
            ) : undefined}
            <ModalRoutes getRoutesWithLocation={getRoutesWithLocation} />
        </>
    );
}

AuthorizedRoutes.propTypes = {
    alertMessage: PropTypes.string,
    locale: PropTypes.string,
};

const mapStateToProps = ({ authentication }) => ({
    alertMessage: authentication.alertMessage,
    locale: authentication.user?.locale,
});

export default connect(mapStateToProps)(AuthorizedRoutes);
