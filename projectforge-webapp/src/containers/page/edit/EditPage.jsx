import PropTypes from 'prop-types';
import React from 'react';
import { Route } from 'react-router-dom';
import DynamicLayout from '../../../components/base/dynamicLayout';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import { Alert, Container, TabContent, TabPane } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import history from '../../../utilities/history';
import { getTranslation } from '../../../utilities/layout';
import { getObjectFromQuery, getServiceURL, handleHTTPErrors } from '../../../utilities/rest';
import style from '../../ProjectForge.module.scss';
import EditHistory from './history';

function EditPage({ match, location }) {
    const { category, id } = match.params;

    const [loading, setLoading] = React.useState(false);
    const [error, setError] = React.useState(undefined);

    const [data, setDataState] = React.useState({});
    const [ui, setUI] = React.useState({});
    const [validationErrors, setValidationErrors] = React.useState([]);
    const [variables, setVariablesState] = React.useState({});

    const loadPage = () => {
        setLoading(false);
        setError(undefined);
        setUI({});
        setDataState({});
        setValidationErrors([]);
        setVariablesState({});

        const params = {
            ...getObjectFromQuery(location.search || ''),
        };

        if (id) {
            params.id = id;
        }

        fetch(
            getServiceURL(`${category}/edit`, params),
            {
                method: 'GET',
                credentials: 'include',
            },
        )
            .then((response) => {
                setLoading(false);
                return response;
            })
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then((
                {
                    data: responseData,
                    ui: responseUI,
                    variables: responseVariables,
                },
            ) => {
                setDataState(responseData);
                setVariablesState(responseVariables);
                setUI(responseUI);
            })
            .catch(setError);
    };

    const setVariables = async (newVariables, callback) => {
        const computedVariables = {
            ...variables,
            ...(typeof newVariables === 'function' ? newVariables(variables) : newVariables),
        };

        setVariablesState(computedVariables);

        if (callback) {
            callback(computedVariables);
        }

        return computedVariables;
    };

    const callAction = ({ responseAction }) => {
        if (!responseAction) {
            return;
        }

        setLoading(true);
        setError(undefined);
        setValidationErrors([]);

        let status = 0;

        fetch(
            getServiceURL(responseAction.url),
            {
                method: responseAction.targetType,
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data),
            },
        )
            .then((response) => {
                setLoading(false);

                // Object Destructuring
                ({ status } = response);

                if (response.status === 200 || response.status === 406) {
                    return response.json();
                }

                throw Error(`Error ${response.status}`);
            })
            .then((json) => {
                switch (status) {
                    case 200:
                        switch (json.targetType) {
                            case 'REDIRECT':
                                history.push(json.url, json.variables);
                                break;
                            case 'UPDATE':
                                history.push(`${json.url}`, { noReload: true });
                                window.scrollTo(0, 0);
                                if (json.variables.variables) {
                                    setVariables(json.variables.variables);
                                }
                                setDataState(json.variables.data);
                                setUI(json.variables.ui);
                                break;
                            default:
                                throw Error(`Target Type ${json.targetType} not implemented`);
                        }
                        break;
                    case 406:
                        setValidationErrors(json.validationErrors);
                        window.scrollTo(0, 0);
                        break;
                    default:
                        throw Error(`Error ${status}`);
                }
            })
            .catch(setError);
    };

    const setData = async (newData, callback) => {
        const computedData = {
            ...data,
            ...(typeof newData === 'function' ? newData(data) : newData),
        };

        setDataState(computedData);

        if (callback) {
            callback(computedData);
        }

        return computedData;
    };

    React.useEffect(() => {
        if (location.state && location.state.noReload && Object.entries(data).length !== 0) {
            return;
        }
        loadPage();
    }, [category, id, location.state]);

    const globalValidation = React.useMemo(() => {
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

    if (error) {
        return (
            <Alert color="danger">
                <h4>[An error occured]</h4>
                <p>{error.message}</p>
            </Alert>
        );
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
        <LoadingContainer loading={loading}>
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
                                            callAction={callAction}
                                            data={data}
                                            options={{
                                                displayPageMenu: id !== undefined,
                                                setBrowserTitle: true,
                                                showActionButtons: true,
                                                showPageMenuTitle: false,
                                            }}
                                            setData={setData}
                                            setVariables={setVariables}
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
                                                category={category}
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
        </LoadingContainer>
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
};

EditPage.defaultProps = {};

export default EditPage;
