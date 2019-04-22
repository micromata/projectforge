import React from 'react';
import PropTypes from 'prop-types';
import 'moment/min/locales';
import 'react-day-picker/lib/style.css';
import 'rc-time-picker/assets/index.css';
import ReactSelect from '../../ReactSelect';
import TaskSelect from '../../TaskSelect';

/**
 * Kost2 selection for editing time sheets.
 */
class TimesheetEditTaskAndKost2 extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            kost2List: undefined,
        };

        this.changeKost2List = this.changeKost2List.bind(this);
    }

    componentDidMount() {
        const { variables } = this.props;
        const task = variables ? variables.task : undefined;
        const kost2List = task ? task.kost2List : undefined;
        this.setState({ kost2List });
    }

    changeKost2List(kost2List) {
        this.setState({ kost2List });
    }

    render() {
        const {
            data,
            variables,
            values,
            changeDataField,
            translations,
        } = this.props;
        const { kost2List } = this.state;

        let kost2Row;
        if (Array.isArray(kost2List) && kost2List.length) {
            kost2Row = (
                <div className="row">
                    <div className="col col-sm-4">
                        <div className="form-group">
                            <ReactSelect
                                label={translations['fibu.kost2']}
                                data={data}
                                id={values.id}
                                values={kost2List}
                                changeDataField={changeDataField}
                                translations={translations}
                                valueProperty="id"
                                labelProperty="title"
                            />
                        </div>
                    </div>
                </div>
            );
        }
        return (
            <React.Fragment>
                <div className="row">
                    <div className="col">
                        <div className="form-group">
                            <TaskSelect
                                changeDataField={changeDataField}
                                data={data}
                                variables={variables}
                                id="task"
                                label={translations.task}
                                onKost2Changed={this.changeKost2List}
                            />
                        </div>
                    </div>
                </div>
                {kost2Row}
            </React.Fragment>
        );
    }
}

TimesheetEditTaskAndKost2.propTypes = {
    changeDataField: PropTypes.func.isRequired,
    values: PropTypes.shape({}).isRequired,
    data: PropTypes.shape({}).isRequired,
    variables: PropTypes.shape({}).isRequired,
    translations: PropTypes.shape({}).isRequired,
};

TimesheetEditTaskAndKost2.defaultProps = {};

export default TimesheetEditTaskAndKost2;
