import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { changeEditFormField, loadEditPage } from '../../../actions';
import DynamicLayout from '../../../components/base/dynamicLayout';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import { Alert, Container, TabContent, TabPane, } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import { getTranslation } from '../../../utilities/layout';
import { getObjectFromQuery } from '../../../utilities/rest';
import style from '../../ProjectForge.module.scss';
import EditHistory from './history';

class EditPage extends React.Component {
    constructor(props) {
        super(props);

        this.setData = this.setData.bind(this);
    }

    componentDidMount() {
        const {
            load,
            location,
            match,
            onClose,
        } = this.props;

        load(
            match.params.category,
            match.params.id,
            getObjectFromQuery(location.search || ''),
            onClose,
        );
    }

    async setData(newData, callback) {
        // Load some props from redux
        const { data, changeDataField } = this.props;

        // compute new data if it's a function.
        const computedNewData = typeof newData === 'function' ? newData(data) : newData;

        Object.keys(computedNewData)
            .forEach(key => changeDataField(key, computedNewData[key]));

        const absoluteNewData = {
            ...data,
            ...computedNewData,
        };

        if (callback) {
            callback(absoluteNewData);
        }

        return absoluteNewData;
    }

    render() {
        const {
            data,
            variables,
            error,
            loading,
            ui,
            match,
        } = this.props;

        const { category, id } = match.params;

        if (error) {
            return (
                <Alert color="danger">
                    <h4>[Failed to Fetch Design]</h4>
                    <p>[Description Here]</p>
                </Alert>
            );
        }

        const activeTab = match.params.tab || 'edit';

        const tabs = [];

        if (id) {
            const baseUrl = `/${category}/edit/${id}`;

            tabs.push({
                id: 'edit',
                title: ui.title,
                link: baseUrl,
            }, {
                id: 'history',
                title: getTranslation('label.historyOfChanges', ui.translations),
                link: `${baseUrl}/history`,
            });
        }

        return (
            <LoadingContainer loading={loading}>
                <TabNavigation
                    tabs={tabs}
                    activeTab={activeTab}
                />
                <TabContent
                    activeTab={activeTab}
                    className={style.tabContent}
                >
                    <TabPane tabId="edit">
                        <Container fluid>
                            <form>
                                <DynamicLayout
                                    data={data}
                                    options={{
                                        displayPageMenu: id !== undefined,
                                        setBrowserTitle: true,
                                        showPageMenuTitle: false,
                                    }}
                                    setData={this.setData}
                                    ui={ui}
                                    variables={variables}
                                />
                            </form>
                        </Container>
                    </TabPane>
                    {id
                        ? (
                            <TabPane tabId="history">
                                <Container fluid>
                                    <EditHistory
                                        category={category}
                                        id={id}
                                        translations={ui.translations}
                                    />
                                </Container>
                            </TabPane>
                        )
                        : undefined}
                </TabContent>
            </LoadingContainer>
        );
    }
}

EditPage.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    load: PropTypes.func.isRequired,
    location: PropTypes.shape({
        hash: PropTypes.string,
        pathname: PropTypes.string,
        search: PropTypes.string,
    }).isRequired,
    match: PropTypes.shape({}).isRequired,
    ui: PropTypes.shape({
        translations: PropTypes.shape({}),
    }).isRequired,
    data: PropTypes.shape({}),
    error: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.number,
        PropTypes.object,
    ]),
    loading: PropTypes.bool,
    onClose: PropTypes.func,
    variables: PropTypes.shape({}),
};

EditPage.defaultProps = {
    data: [],
    error: undefined,
    loading: false,
    onClose: undefined,
    variables: {},
};

const mapStateToProps = state => ({
    data: state.editPage.data,
    error: state.editPage.error,
    loading: state.editPage.loading,
    ui: state.editPage.ui,
    variables: state.editPage.variables,
});

const actions = {
    changeDataField: changeEditFormField,
    load: loadEditPage,
};

export default connect(mapStateToProps, actions)(EditPage);
