import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Route } from 'react-router-dom';
import { callAction, loadEditPage, setCurrentData, setCurrentVariables } from '../../../actions';
import DynamicLayout from '../../../components/base/dynamicLayout';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import { Alert, Container, TabContent, TabPane } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import { getTranslation } from '../../../utilities/layout';
import style from '../../ProjectForge.module.scss';
import EditHistory from './history';

function EditPage(
    {
        category,
        location,
        match,
        onCallAction,
        onDataChange,
        onNewEditPage,
        onVariablesChange,
    },
) {
    const {
        data,
        ui,
        validationErrors,
        variables,
    } = category;
    const { category: currentCategory, id } = match.params;

    React.useEffect(
        () => {
            if (location.state && location.state.noReload && Object.entries(data).length !== 0) {
                return;
            }

            onNewEditPage(currentCategory, {
                id,
                search: location.search,
            });
        },
        [currentCategory, id, location.state],
    );

    const globalValidation = React.useMemo(() => {
        if (validationErrors === undefined) {
            return <React.Fragment />;
        }
        const globalErrors = validationErrors.filter(entry => entry.fieldId === undefined);

        if (globalErrors.length === 0) {
            return <React.Fragment />;
        }

        return (
            <Alert color="danger">
                <ul>
                    {globalErrors.map(({ message, messageId }) => (
                        <li key={`edit-page-global-validation-${messageId}`}>
                            {message}
                        </li>
                    ))}
                </ul>
            </Alert>
        );
    }, [validationErrors]);

    if (ui === undefined) {
        return <LoadingContainer loading />;
    }

    const tabs = [
        {
            id: 'edit',
            title: ui.title,
            link: match.url,
        },
    ];

    if (ui.showHistory === true) {
        tabs.push({
            id: 'history',
            title: getTranslation('label.historyOfChanges', ui.translations),
            link: `${match.url}/history`,
        });
    }

    return (
        <Container fluid>
            <Route
                path={`${match.url}/:tab?`}
                render={({ match: tabMatch }) => (
                    <React.Fragment>
                        <TabNavigation
                            tabs={tabs}
                            activeTab={tabMatch.params.tab || 'edit'}
                        />
                        <TabContent
                            activeTab={tabMatch.params.tab || 'edit'}
                            className={style.tabContent}
                        >
                            <TabPane tabId="edit">
                                <Container fluid>
                                    <form>
                                        <DynamicLayout
                                            callAction={onCallAction}
                                            data={data}
                                            options={{
                                                displayPageMenu: id !== undefined,
                                                setBrowserTitle: true,
                                                showActionButtons: true,
                                                showPageMenuTitle: false,
                                            }}
                                            setData={onDataChange}
                                            setVariables={onVariablesChange}
                                            ui={ui}
                                            validationErrors={validationErrors}
                                            variables={variables}
                                        >
                                            {globalValidation}
                                        </DynamicLayout>
                                    </form>
                                </Container>
                            </TabPane>
                            {ui.showHistory === true && id
                            && (
                                <TabPane tabId="history">
                                    <Container fluid>
                                        <EditHistory
                                            category={currentCategory}
                                            id={id}
                                            translations={ui.translations}
                                            visible={tabMatch.params.tab === 'history'}
                                        />
                                    </Container>
                                </TabPane>
                            )}
                        </TabContent>
                    </React.Fragment>
                )}
            />
        </Container>
    );
}

EditPage.propTypes = {
    location: PropTypes.shape({
        search: PropTypes.string,
        state: PropTypes.shape({
            noReload: PropTypes.bool,
        }),
    }).isRequired,
    match: PropTypes.shape({
        params: PropTypes.shape({
            category: PropTypes.string.isRequired,
            id: PropTypes.string,
            tab: PropTypes.string,
        }).isRequired,
    }).isRequired,
    onCallAction: PropTypes.func.isRequired,
    onDataChange: PropTypes.func.isRequired,
    onNewEditPage: PropTypes.func.isRequired,
    onVariablesChange: PropTypes.func.isRequired,
    category: PropTypes.shape({}),
};

EditPage.defaultProps = {
    category: {},
};

const mapStateToProps = ({ edit }, { match }) => ({
    category: edit.categories[match.params.category],
});

const actions = {
    onCallAction: callAction,
    onDataChange: setCurrentData,
    onNewEditPage: loadEditPage,
    onVariablesChange: setCurrentVariables,
};

export default connect(mapStateToProps, actions)(EditPage);
