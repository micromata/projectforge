import { faBell, faExclamationTriangle } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import PropTypes from 'prop-types';
import React from 'react';
import {
    Alert,
    Button,
    Card,
    CardBody,
    CardTitle,
    CheckBox,
    Form,
    Input,
} from '../design';
import LoadingContainer from '../design/loading-container';
import style from './Authentication.module.scss';

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
                                <Alert
                                    color="danger"
                                    className={style.alert}
                                    aria-label="administratorLoginNeededAlert"
                                >
                                    <FontAwesomeIcon icon={faBell} />
                                    [login.adminLoginRequired]
                                </Alert>
                            )
                            : undefined
                        }
                        {error
                            ? (
                                <Alert
                                    color="danger"
                                    className={style.alert}
                                    aria-label="errorAlert"
                                >
                                    <FontAwesomeIcon icon={faBell} />
                                    {` ${error}`}
                                </Alert>
                            )
                            : undefined

                        }
                        {motd
                            ? (
                                <Alert
                                    color="primary"
                                    className={style.alert}
                                    aria-label="motdAlert"
                                >
                                    <FontAwesomeIcon icon={faExclamationTriangle} />
                                    {` ${motd}`}
                                </Alert>
                            )
                            : undefined
                        }
                        <Form onSubmit={this.handleFormSubmit}>
                            <Input
                                label="[Username]"
                                id="username"
                                onChange={this.handleInputChange}
                                value={username}
                                autoComplete="username"
                                aria-label="username"
                                className={style.formGroup}
                            />
                            <Input
                                label="[Passwort]"
                                id="password"
                                onChange={this.handleInputChange}
                                value={password}
                                autoComplete="current-password"
                                aria-label="password"
                                type="password"
                                className={style.formGroup}
                            />
                            <CheckBox
                                id="keepSignedIn"
                                label="[angemeldet bleiben]"
                                checked={keepSignedIn}
                                onChange={this.handleInputChange}
                                aria-label="keepSignedIn"
                                color="primary"
                                className={style.formGroup}
                            />
                            <Button color="success" block aria-label="login">[Login]</Button>
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
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    motd: PropTypes.string,
};

LoginView.defaultProps = {
    administratorLoginNeeded: false,
    error: undefined,
    motd: undefined,
};

export default LoginView;
