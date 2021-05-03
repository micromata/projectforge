import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from '../../../../components/design';
import LoadingContainer from '../../../../components/design/loading-container';
import { getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import HistoryEntry from './HistoryEntry';

class FormHistory extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loading: true,
            history: [],
            initialized: false,
        };

        this.loadHistory = this.loadHistory.bind(this);
    }

    componentDidMount() {
        const { visible } = this.props;
        if (visible) {
            // History page of edit page is opened first (user hits reload button on this tab or
            // used the history url directly: {category}/edit/{id}/history
            this.loadHistory();
        }
    }

    shouldComponentUpdate(nextProps, nextState) {
        const { initialized } = nextState;
        if (initialized) {
            // Do not load, if already initialized.
            return true;
        }
        const { visible: nextVisible } = nextProps;
        const { visible } = this.props;
        if (!visible && nextVisible) {
            // Component wasn't visible, but will be visible.
            this.setState({ initialized: true }, this.loadHistory);
            // Don't render component, will be rendered after state changed to initialized.
            return false;
        }
        return true; // Render component.
    }

    loadHistory() {
        const { category, id } = this.props;

        this.setState({
            loading: true,
            initialized: true,
            error: undefined,
        });

        fetch(
            getServiceURL(`${category}/history/${id}`),
            {
                method: 'GET',
                credentials: 'include',
            },
        )
            .then(handleHTTPErrors)
            .then((response) => response.json())
            .then((json) => this.setState({
                loading: false,
                history: json,
            }))
            .catch((error) => this.setState({
                loading: false,
                error,
            }));
    }

    render() {
        const { translations } = this.props;
        const { loading, error, history } = this.state;

        if (error) {
            return (
                <Alert color="danger">
                    <h4>[Failed to fetch History]</h4>
                    <p>{error.message}</p>
                </Alert>
            );
        }

        return (
            <LoadingContainer loading={loading}>
                {history.map((entry) => (
                    <HistoryEntry
                        key={`history-entry-at-${entry.modifiedAt}`}
                        entry={entry}
                        translations={translations}
                    />
                ))}
            </LoadingContainer>
        );
    }
}

FormHistory.propTypes = {
    category: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    visible: PropTypes.bool,
    translations: PropTypes.shape({
        changes: PropTypes.string,
    }),
};

FormHistory.defaultProps = {
    translations: undefined,
    visible: false,
};

export default FormHistory;
