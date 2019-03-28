import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { changeEditFormField, loadEditPage } from '../../../actions';
import ActionGroup from '../../../components/base/page/action/Group';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import LayoutGroup from '../../../components/base/page/layout/Group';
import PageNavigation from '../../../components/base/page/Navigation';
import { Alert, Container, TabContent, TabPane, } from '../../../components/design';
import LoadingContainer from '../../../components/design/loading-container';
import style from '../../ProjectForge.module.scss';
import EditHistory from './history';

class EditPage extends React.Component {
    componentDidMount() {
        const { load, match } = this.props;

        load(match.params.category, match.params.id);
    }

    render() {
        const {
            changeDataField,
            data,
            error,
            loading,
            ui,
            validation,
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
        const baseUrl = `/${category}/edit/${id}`;

        // TODO: REMOVE HISTORY ON NEW BOOK

        return (
            <LoadingContainer loading={loading}>
                <PageNavigation current={ui.title} />
                <TabNavigation
                    tabs={[
                        {
                            id: 'edit',
                            title: ui.title,
                            link: baseUrl,
                        },
                        {
                            id: 'history',
                            title: '[History]',
                            link: `${baseUrl}/history`,
                        },
                    ]}
                    activeTab={activeTab}
                />
                <TabContent
                    activeTab={activeTab}
                    className={style.tabContent}
                >
                    <TabPane tabId="edit">
                        <Container fluid>
                            <LayoutGroup
                                content={ui.layout}
                                data={data}
                                changeDataField={changeDataField}
                                validation={validation}
                            />
                            <ActionGroup actions={ui.actions} />
                        </Container>
                    </TabPane>
                    <TabPane tabId="history">
                        <Container fluid>
                            <EditHistory
                                category={category}
                                id={id}
                            />
                        </Container>
                    </TabPane>
                </TabContent>
            </LoadingContainer>
        );
    }
}

EditPage.propTypes = {
    category: PropTypes.string.isRequired,
    changeDataField: PropTypes.func.isRequired,
    match: PropTypes.shape({}).isRequired,
    load: PropTypes.func.isRequired,
    ui: PropTypes.shape({}).isRequired,
    validation: PropTypes.shape({}),
    error: PropTypes.string,
    data: PropTypes.shape({}),
    loading: PropTypes.bool,
};

EditPage.defaultProps = {
    data: [],
    error: undefined,
    loading: false,
    validation: {},
};

const mapStateToProps = state => ({
    ui: state.editPage.ui,
    error: state.editPage.error,
    loading: state.editPage.loading,
    data: state.editPage.data,
    validation: state.editPage.validation,
    category: state.editPage.category,
});

const actions = {
    load: loadEditPage,
    changeDataField: changeEditFormField,
};

export default connect(mapStateToProps, actions)(EditPage);
