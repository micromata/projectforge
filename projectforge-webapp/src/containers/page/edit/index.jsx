import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { Container, TabContent, TabPane } from '../../../components/design';
import PageNavigation from '../../../components/base/page/Navigation';
import LoadingContainer from '../../../components/design/loading-container';
import TabNavigation from '../../../components/base/page/edit/TabNavigation';
import { getAuthenticationHeaders, handleHTTPErrors } from '../../../utilities/rest';
import LayoutGroup from '../../../components/base/page/edit/layout/Group';

class EditPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loading: true,
            activeTab: 'edit',
            error: undefined,
            layout: [],
        };

        this.toggleTab = this.toggleTab.bind(this);
    }

    componentDidMount() {
        const { userId, token } = this.props;

        fetch(
            '/rest_examples/edit/layout_example_response.json',
            {
                method: 'GET',
                headers: getAuthenticationHeaders(userId, token),
            },
        )
            .then(handleHTTPErrors)
            .then(response => response.json())
            .then(json => this.setState({
                loading: false,
                layout: json.layout,
            }))
            .catch(error => this.setState({
                error,
                loading: false,
            }));
    }

    toggleTab(event) {
        const { activeTab } = this.state;

        if (activeTab === event.target.id) {
            return;
        }

        this.setState({
            activeTab: event.target.id,
        });
    }

    render() {
        const { loading, activeTab, layout } = this.state;

        // TODO: CATCH ERROR

        return (
            <React.Fragment>
                <PageNavigation />
                <LoadingContainer loading={loading}>
                    <TabNavigation
                        tabs={{
                            edit: '[Bearbeiten]',
                            history: '[History]',
                        }}
                        toggleTab={this.toggleTab}
                        activeTab={activeTab}
                    />
                    <TabContent
                        activeTab={activeTab}
                        style={{
                            background: '#fff',
                            padding: '15px 0',
                        }}
                    >
                        <TabPane tabId="edit">
                            <Container fluid>
                                <LayoutGroup content={layout} />
                            </Container>
                        </TabPane>
                    </TabContent>
                </LoadingContainer>
            </React.Fragment>
        );
    }
}

EditPage.propTypes = {
    userId: PropTypes.string.isRequired,
    token: PropTypes.string.isRequired,
};

const mapStateToProps = state => ({
    userId: state.authentication.user.id,
    token: state.authentication.user.token,
});

export default connect(mapStateToProps)(EditPage);
