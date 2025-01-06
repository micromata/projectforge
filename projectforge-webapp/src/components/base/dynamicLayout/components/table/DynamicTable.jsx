import PropTypes from 'prop-types';
import React from 'react';
import useInterval from '@use-hooks/interval';
import { Table } from '../../../../design';
import { DynamicLayoutContext } from '../../context';
import { fetchJsonGet, fetchJsonPost } from '../../../../../utilities/rest';
import DynamicTableHead from './DynamicTableHead';
import DynamicTableRow from './DynamicTableRow';

function DynamicTable(
    {
        columns,
        id,
        rowClickPostUrl,
        refreshUrl,
        refreshMethod,
        refreshIntervalSeconds = 10,
        autoRefreshFlag,
    },
) {
    const {
        data,
        ui,
        variables,
        setVariables,
    } = React.useContext(DynamicLayoutContext);

    const entries = Object.getByString(data, id) || Object.getByString(variables, id) || '';

    /*
      If automatic refresh is required, the table content must be given as variable in variables.
      Reason: If given in data, the table content would be posted by every refresh.
     */
    React.useEffect(() => {
        if (!refreshUrl) return undefined;

        if (refreshMethod === 'SSE') {
            let eventSource = null;

            const connect = () => {
                // console.log('Opening SSE connection...');
                eventSource = new EventSource(refreshUrl);

                eventSource.onmessage = (event) => {
                    if (event.data !== 'ping') {
                        const json = JSON.parse(event.data); // Parse JSON from server.
                        setVariables({ [id]: json }); // Update the table content.
                    } else {
                        // console.log('Ping');
                    }
                };

                eventSource.onerror = (error) => {
                    // console.error('SSE connection error:', error);
                    eventSource.close(); // Close connection on error
                    setTimeout(() => connect(), 5000); // Attempt reconnect after 5 seconds
                };
            };

            connect(); // Initial connection

            // Cleanup: Close connection.
            return () => {
                // console.log('SSE connection closed.');
                if (eventSource) {
                    eventSource.close();
                }
            };
        }
        const interval = setInterval(() => {
            if (!autoRefreshFlag || Object.getByString(data, autoRefreshFlag)) {
                // eslint-disable-next-line max-len
                const fetchFunction = refreshMethod === 'POST' ? fetchJsonPost : fetchJsonGet;
                fetchFunction(
                    refreshUrl,
                    refreshMethod === 'POST' ? { data } : {},
                    (json) => {
                        setVariables({ [id]: json });
                    },
                );
            }
        }, refreshIntervalSeconds * 1000);

        return () => clearInterval(interval);
        // eslint-disable-next-line max-len
    }, [refreshUrl, refreshMethod, refreshIntervalSeconds, autoRefreshFlag, data, id, setVariables]);

    return React.useMemo(() => (
        <div
            style={{
                marginLeft: '1em',
                marginRight: '1em',
            }}
        >
            <Table striped hover responsive>
                <thead>
                    <tr>
                        {columns.map((column) => (
                            <DynamicTableHead
                                key={`table-head-column-${column.id}`}
                                {...column}
                            />
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {entries && entries.map((row) => (
                        <DynamicTableRow
                            key={`table-body-row-${row.id}`}
                            columns={columns}
                            row={row}
                            highlightRow={data.highlightRowId === row.id}
                            rowClickPostUrl={rowClickPostUrl}
                        />
                    ))}
                </tbody>
            </Table>
            {data.size !== undefined && (
                <p>
                    {`${ui.translations['table.showing']} ${data.size}/${data.totalSize}`}
                </p>
            )}
        </div>
    ), [entries, ui]);
}

DynamicTable.propTypes = {
    columns: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.string.isRequired,
        title: PropTypes.string,
        titleIcon: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    id: PropTypes.string,
    rowClickPostUrl: PropTypes.string,
    refreshUrl: PropTypes.string,
    refreshIntervalSeconds: PropTypes.number,
    autoRefreshFlag: PropTypes.string,
};

export default DynamicTable;
