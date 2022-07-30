import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import { Card, CardBody, CardHeader, CardText, CardTitle } from 'reactstrap';
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
            <Card>
                <CardHeader>
                    <CardTitle>
                        {translations['index.welcome']}
                    </CardTitle>
                </CardHeader>
                <CardBody>
                    <CardText>
                        {translations['index.website']}
                        {': '}
                        <a href="https://www.projectforge.org" target="_blank" rel="noreferrer">www.projectforge.org</a>
                    </CardText>
                    <CardText>
                        {translations['index.development']}
                        {': '}
                        <a href="https://github.com/micromata/projectforge/" target="_blank" rel="noreferrer">github.com/micromata/projectforge/</a>
                    </CardText>
                </CardBody>
            </Card>
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
