import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import format from '../../../../../utilities/format';
import history from '../../../../../utilities/history';
import { tableColumnsPropType } from '../../../../../utilities/propTypes';
import style from '../../Page.module.scss';

class TableRow extends React.Component {
    constructor(props) {
        super(props);

        this.handleRowClick = this.handleRowClick.bind(this);
    }

    handleRowClick() {
        const { data, category } = this.props;

        history.push(`/${category}/edit/${data.id}`);
    }

    render() {
        const { columns, data } = this.props;

        return (
            <tr
                onClick={this.handleRowClick}
                className={style.clickable}
            >
                {columns.map(column => (
                    <td key={`table-body-row-${data.id}-column-${column.id}`}>
                        {column.formatter
                            ? format(column.formatter, data[column.id])
                            : data[column.id]}
                    </td>
                ))}
            </tr>
        );
    }
}

TableRow.propTypes = {
    category: PropTypes.string.isRequired,
    columns: tableColumnsPropType.isRequired,
    data: PropTypes.shape({
        id: PropTypes.number,
    }).isRequired,
};

const mapStateToProps = state => ({
    category: state.listPage.category,
});

export default connect(mapStateToProps)(TableRow);
