import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { ListPageContext } from '../../../../../containers/page/list/ListPageContext';
import { tableColumnsPropType } from '../../../../../utilities/propTypes';
import Formatter from '../../../Formatter';
import DynamicCustomized from '../customized';
import style from './DynamicTable.module.scss';

function DynamicTableRow({ columns, row }) {
    const { category, highlightRow, openEditPage } = React.useContext(ListPageContext);

    return React.useMemo(() => {
        const handleRowClick = () => openEditPage(row.id);

        return (
            <tr
                onClick={handleRowClick}
                className={classNames(
                    style.clickable,
                    { [style.highlighted]: highlightRow === row.id },
                )}
            >
                {columns.map((
                    {
                        id,
                        formatter,
                        dataType,
                    },
                ) => (
                    <td key={`table-body-row-${row.id}-column-${id}`}>
                        {dataType === 'CUSTOMIZED'
                            ? <DynamicCustomized id={id} data={row} />
                            : (
                                <Formatter
                                    formatter={formatter}
                                    data={row}
                                    id={id}
                                    dataType={dataType}
                                />
                            )}
                    </td>
                ))}
            </tr>
        );
    }, [category, columns, row]);
}

DynamicTableRow.propTypes = {
    columns: tableColumnsPropType.isRequired,
    row: PropTypes.shape({
        id: PropTypes.number.isRequired,
    }).isRequired,
};

DynamicTableRow.defaultProps = {};

export default DynamicTableRow;
