import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { tableColumnsPropType } from '../../../../../utilities/propTypes';
import Formatter from '../../../Formatter';
import DynamicCustomized from '../customized';
import style from './DynamicTable.module.scss';
import { DynamicLayoutContext } from '../../context';
import { getServiceURL, handleHTTPErrors } from '../../../../../utilities/rest';

function DynamicTableRow(
    {
        columns,
        row,
        highlightRow = false,
        rowClickPostUrl,
    },
) {
    const {
        callAction,
        data,
        setData,
    } = React.useContext(DynamicLayoutContext);

    const { template } = data;

    const handleRowClick = () => (event) => {
        event.stopPropagation();
        fetch(
            getServiceURL(`${rowClickPostUrl}/${row.id}`),
            {
                credentials: 'include',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Accept: 'application/json',
                },
                body: JSON.stringify({
                    data,
                }),
            },
        )
            .then(handleHTTPErrors)
            .then((body) => body.json())
            .then((json) => {
                callAction({ responseAction: json });
            })
            // eslint-disable-next-line no-alert
            .catch((error) => alert(`Internal error: ${error}`));
    };

    return React.useMemo(() => (
        <tr
            className={classNames(
                style.clickable,
                { [style.highlighted]: highlightRow === true },
                { [style.deleted]: row.deleted === true },
            )}
            onClick={rowClickPostUrl ? handleRowClick() : undefined}
        >
            {columns.map((
                {
                    id,
                    dataType,
                    ...column
                },
            ) => (
                <td key={`table-body-row-${row.id}-column-${id}`}>
                    {dataType === 'CUSTOMIZED'
                        ? <DynamicCustomized id={id} data={row} />
                        : (
                            <Formatter
                                data={row}
                                id={id}
                                dataType={dataType}
                                {...column}
                            />
                        )}
                </td>
            ))}
        </tr>
    ), [columns, setData, template]);
}

DynamicTableRow.propTypes = {
    columns: tableColumnsPropType.isRequired,
    row: PropTypes.shape({
        id: PropTypes.number.isRequired,
        deleted: PropTypes.bool,
    }).isRequired,
    highlightRow: PropTypes.bool,
    rowClickPostUrl: PropTypes.string,
};

export default DynamicTableRow;
