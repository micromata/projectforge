import PropTypes from 'prop-types';
import React, { useEffect } from 'react';
import { connect } from 'react-redux';
import { Route, Routes, useLocation } from 'react-router';
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
import RedirectToNext from './RedirectToNext';
import FormModal from './page/form/FormModal';
import MenuCustomizerPanel from './panel/menu/MenuCustomizerPanel';

export const wicketRoute = (
    <Route
        path="/wa/*"
        element={<RedirectToWicket />}
    />
);

// Pages already migrated to projectforge-next; needs a real page load, not client-side routing.
export const nextRoute = (
    <Route
        path="/next/*"
        element={<RedirectToNext />}
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
        legacyBannerText,
        legacyBannerFeedbackText,
        migratedCategories,
        locale = 'en',
    },
) {
    const { pathname } = useLocation();

    useEffect(() => {
        document.documentElement.lang = locale;
    }, [locale]);

    // Show the old-version hint only where there is a new version to point to: the first
    // path segment after /react/ is the page's category (the legacy app mounts pages under
    // <category>), so it is migrated exactly when that segment is in the set the server sent.
    const category = pathname.startsWith(prefix)
        ? pathname.substring(prefix.length).split('/')[0]
        : undefined;
    const showLegacyBanner = Boolean(
        legacyBannerText && category && migratedCategories?.includes(category),
    );

    const getRoutesWithLocation = (location) => (
        <Routes location={location}>
            {wicketRoute}
            {nextRoute}
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
                path={`${prefix}customizeMenu`}
                element={<MenuCustomizerPanel />}
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
            <GlobalNavigation />
            {showLegacyBanner ? (
                <Container fluid>
                    <Alert color="warning">
                        {legacyBannerText}
                        {legacyBannerFeedbackText ? (
                            <>
                                <br />
                                <a className="alert-link" href="/next/feedback">
                                    {legacyBannerFeedbackText}
                                </a>
                            </>
                        ) : undefined}
                    </Alert>
                </Container>
            ) : undefined}
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
    legacyBannerText: PropTypes.string,
    legacyBannerFeedbackText: PropTypes.string,
    migratedCategories: PropTypes.arrayOf(PropTypes.string),
    locale: PropTypes.string,
};

const mapStateToProps = ({ authentication }) => ({
    alertMessage: authentication.alertMessage,
    legacyBannerText: authentication.legacyBannerText,
    legacyBannerFeedbackText: authentication.legacyBannerFeedbackText,
    migratedCategories: authentication.migratedCategories,
    locale: authentication.user?.locale,
});

export default connect(mapStateToProps)(AuthorizedRoutes);
