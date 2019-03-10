import React from 'react';
import PropTypes from 'prop-types';
import { faBell, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    Alert,
    Button,
    Card,
    CardBody,
    CardTitle,
    Col,
    Form,
    FormGroup,
    Input,
    Label,
} from '../design';
import style from './Authentication.module.scss';
import LoadingContainer from '../design/loading-container';

// TODO: INCLUDE TRANSLATION
class LoginView extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            username: '',
            password: '',
            keepSignedIn: false,
        };

        this.handleInputChange = this.handleInputChange.bind(this);
        this.handleFormSubmit = this.handleFormSubmit.bind(this);
    }

    handleInputChange(event) {
        const {
            id,
            value,
            type,
            checked,
        } = event.target;

        this.setState({
            [id]: type === 'checkbox' ? checked : value,
        });
    }

    handleFormSubmit(event) {
        event.preventDefault();

        const { login } = this.props;
        const { username, password, keepSignedIn } = this.state;
        login(username, password, keepSignedIn);
    }

    render() {
        const {
            motd,
            administratorLoginNeeded,
            error,
            loading,
        } = this.props;
        const { username, password, keepSignedIn } = this.state;

        return (
            <LoadingContainer className={style.loginPanel} loading={loading}>
                <Card>
                    <CardBody>
                        <CardTitle className={style.cardTitle}>[login.title]</CardTitle>
                        {administratorLoginNeeded
                            ? (
                                <Alert color="danger" className={style.alert}>
                                    <FontAwesomeIcon icon={faBell} />
                                    [login.adminLoginRequired]
                                </Alert>
                            )
                            : undefined
                        }
                        {error
                            ? (
                                <Alert color="danger" className={style.alert}>
                                    <FontAwesomeIcon icon={faBell} />
                                    {` ${error}`}
                                </Alert>
                            )
                            : undefined

                        }
                        {motd
                            ? (
                                <Alert color="primary" className={style.alert}>
                                    <FontAwesomeIcon icon={faExclamationTriangle} />
                                    {` ${motd}`}
                                </Alert>
                            )
                            : undefined
                        }
                        <Form onSubmit={this.handleFormSubmit}>
                            <FormGroup row className={style.formGroup}>
                                <Label for="username" sm={3}><strong>[Username]</strong></Label>
                                <Col sm={9}>
                                    <Input
                                        type="text"
                                        name="username"
                                        id="username"
                                        placeholder="[Username]"
                                        onChange={this.handleInputChange}
                                        value={username}
                                    />
                                </Col>
                            </FormGroup>
                            <FormGroup row className={style.formGroup}>
                                <Label for="password" sm={3}><strong>[Password]</strong></Label>
                                <Col sm={9}>
                                    <Input
                                        type="password"
                                        name="password"
                                        id="password"
                                        placeholder="[Password]"
                                        onChange={this.handleInputChange}
                                        value={password}
                                    />
                                </Col>
                            </FormGroup>
                            <FormGroup row className={style.formGroup}>
                                <Col
                                    sm={{
                                        size: 9,
                                        offset: 3,
                                    }}
                                >
                                    <FormGroup check>
                                        <Label check>
                                            <Input
                                                type="checkbox"
                                                id="keepSignedIn"
                                                onChange={this.handleInputChange}
                                                value={keepSignedIn}
                                            />
                                            <strong>[Keep Signed In]</strong>
                                        </Label>
                                    </FormGroup>
                                </Col>
                            </FormGroup>
                            <Button color="success" block>[Login]</Button>
                        </Form>
                    </CardBody>
                </Card>
            </LoadingContainer>
        );
    }
}

LoginView.propTypes = {
    loading: PropTypes.bool.isRequired,
    login: PropTypes.func.isRequired,
    administratorLoginNeeded: PropTypes.bool,
    error: PropTypes.string,
    motd: PropTypes.string,
};

LoginView.defaultProps = {
    administratorLoginNeeded: false,
    error: undefined,
    motd: undefined,
};

export default LoginView;
