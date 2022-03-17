import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Card, CardBody, CardHeader, CardText, CardTitle, Col, Container, Row } from 'reactstrap';
import { loadUserStatus } from '../../actions';
import { getServiceURL } from '../../utilities/rest';

class IndexPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            translations: undefined,
        };

        this.fetchInitial = this.fetchInitial.bind(this);
    }

    componentDidMount() {
        this.fetchInitial();
    }

    componentDidUpdate({ location: nextLocation }) {
        const { location, loadUserStatus: checkAuthentication } = this.props;

        if (location.key === nextLocation.key) {
            return;
        }

        checkAuthentication();
    }

    fetchInitial() {
        const { loadUserStatus: checkAuthentication } = this.props;

        fetch(getServiceURL('index'), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then((response) => {
                if (response.status === 401) {
                    throw response.status;
                }

                return response.json();
            })
            .then((json) => {
                const {
                    translations,
                } = json;
                this.setState({
                    translations,
                });
            })
            .catch(() => checkAuthentication());
    }

    render() {
        const { translations } = this.state;

        if (!translations) {
            return (<div>{' '}</div>);
        }
        return (
            <Container>
                <Row>
                    <Col>
                        <a href="/wa/">
                            <Card>
                                <CardHeader>
                                    <CardTitle>
                                        {translations['goreact.index.classics.header']}
                                    </CardTitle>
                                </CardHeader>
                                <CardBody>
                                    <CardText>
                                        {translations['goreact.index.classics.body1']}
                                    </CardText>
                                    <CardText>
                                        {translations['goreact.index.classics.body2']}
                                    </CardText>
                                </CardBody>
                            </Card>
                        </a>
                    </Col>
                    <Col>
                        <Card>
                            <CardHeader>
                                <CardTitle>
                                    {translations['goreact.index.react.header']}
                                </CardTitle>
                            </CardHeader>
                            <CardBody>
                                <CardText>
                                    {translations['goreact.index.react.body1']}
                                </CardText>
                                <CardText>
                                    {translations['goreact.index.react.body2']}
                                </CardText>
                            </CardBody>
                        </Card>
                    </Col>
                    <Col>
                        <Card>
                            <CardHeader>
                                <CardTitle>
                                    {translations['goreact.index.both.header']}
                                </CardTitle>
                            </CardHeader>
                            <CardBody>
                                <CardText>
                                    {translations['goreact.index.both.body1']}
                                </CardText>
                                <CardText>
                                    {translations['goreact.index.both.body2']}
                                </CardText>
                            </CardBody>
                        </Card>
                    </Col>
                </Row>
            </Container>
        );
    }
}

IndexPage.propTypes = {
    loadUserStatus: PropTypes.func.isRequired,
    location: PropTypes.shape({
        key: PropTypes.string,
    }).isRequired,
};

IndexPage.defaultProps = {};

const actions = {
    loadUserStatus,
};

export default connect(undefined, actions)(IndexPage);
