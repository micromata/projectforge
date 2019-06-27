import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import history from '../../../../../utilities/history';
import { tableColumnsPropType } from '../../../../../utilities/propTypes';
import Formatter from '../../../Formatter';
import style from '../../../page/Page.module.scss';
import DynamicCustomized from '../customized';

function DynamicTableRow({ category, columns, row }) {
    const handleRowClick = () => history.push(`/${category}/edit/${row.id}`);

    return (
        <tr
            onClick={handleRowClick}
            className={style.clickable}
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
                        ? <DynamicCustomized id={id} />
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
}

DynamicTableRow.propTypes = {
    category: PropTypes.string.isRequired,
    columns: tableColumnsPropType.isRequired,
    row: PropTypes.shape({
        id: PropTypes.number.isRequired,
    }).isRequired,
};

DynamicTableRow.defaultProps = {};

const mapStateToProps = state => ({
    category: state.listPage.category,
});

export default connect(mapStateToProps)(DynamicTableRow);
