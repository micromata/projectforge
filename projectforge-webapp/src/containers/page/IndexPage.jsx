import React from 'react';
import { Card, CardBody, CardHeader, CardText, CardTitle, Col, Container, Row } from 'reactstrap';
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

    fetchInitial() {
        fetch(getServiceURL('index'), {
            method: 'GET',
            credentials: 'include',
            headers: {
                Accept: 'application/json',
            },
        })
            .then(response => response.json())
            .then((json) => {
                const {
                    translations,
                } = json;
                this.setState({
                    translations,
                });
            })
            .catch(error => alert(`Internal error: ${error}`));
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

IndexPage.propTypes = {};

IndexPage.defaultProps = {};

export default IndexPage;
