import PropTypes from 'prop-types';
import React from 'react';
import { Alert } from '../../../../components/design';
import LoadingContainer from '../../../../components/design/loading-container';
import { getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import HistoryEntry from './HistoryEntry';

class EditHistory extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loading: true,
            history: [],
        };

        this.loadHistory = this.loadHistory.bind(this);
    }

    componentDidMount() {
        this.loadHistory();
    }

    loadHistory() {
        const { category, id } = this.props;

        this.setState({
            loading: true,
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
            .then(response => response.json())
            .then(json => this.setState({
                loading: false,
                history: json,
            }))
            .catch(error => this.setState({
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
                {history.map(entry => (
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

EditHistory.propTypes = {
    category: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    translations: PropTypes.shape({
        changes: PropTypes.string,
    }),
};

EditHistory.defaultProps = {
    translations: undefined,
};

export default EditHistory;
