import PropTypes from 'prop-types';
import React, { useState, useEffect } from 'react';
import { Alert } from '../../../../components/design';
import LoadingContainer from '../../../../components/design/loading-container';
import { getServiceURL, handleHTTPErrors } from '../../../../utilities/rest';
import HistoryEntry from './HistoryEntry';

function FormHistory({
    category,
    id,
    visible = false,
    translations,
    userAccess,
}) {
    const [loading, setLoading] = useState(true);
    const [history, setHistory] = useState([]);
    const [initialized, setInitialized] = useState(false);
    const [error, setError] = useState();

    const loadHistory = async () => {
        setLoading(true);
        setInitialized(true);
        setError(undefined);

        try {
            const response = await fetch(
                getServiceURL(`${category}/history/${id}`),
                {
                    method: 'GET',
                    credentials: 'include',
                },
            );
            const data = await handleHTTPErrors(response);
            const json = await data.json();

            setHistory(json);
        } catch (err) {
            setError(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (visible && !initialized) {
            loadHistory();
        }
    }, [visible, initialized]);

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
                    userAccess={userAccess}
                />
            ))}
        </LoadingContainer>
    );
}

FormHistory.propTypes = {
    category: PropTypes.string.isRequired,
    id: PropTypes.string.isRequired,
    visible: PropTypes.bool,
    translations: PropTypes.shape({
        changes: PropTypes.string,
    }),
    userAccess: PropTypes.shape({
        editHistoryComments: PropTypes.bool,
    }),
};

export default FormHistory;
