import PropTypes from 'prop-types';
import React from 'react';
import { connect } from 'react-redux';
import history from '../../../../../utilities/history';
import { tableColumnsPropType } from '../../../../../utilities/propTypes';
import Formatter from '../../../Formatter';
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
        const { columns, data, variables } = this.props;

        return (
            <tr
                onClick={this.handleRowClick}
                className={style.clickable}
            >
                {columns.map(({ id, formatter, dataType }) => {
                    /*
                    let element;
                    if (dataType === 'CUSTOMIZED') {
                        element = <CustomizedLayout id={id} data={data} variables={variables} />;
                    } else {
                        element = (
                            <Formatter
                                formatter={formatter}
                                data={data}
                                id={id}
                                dataType={dataType}
                            />
                        );
                    }
                    */
                    return (
                        <td key={`table-body-row-${data.id}-column-${id}`}>
                            <Formatter
                                formatter={formatter}
                                data={data}
                                id={id}
                                dataType={dataType}
                            />
                        </td>
                    );
                })}
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
    variables: PropTypes.shape({}),
};

TableRow.defaultProps = {
    variables: undefined,
};

const mapStateToProps = state => ({
    category: state.listPage.category,
});

export default connect(mapStateToProps)(TableRow);
