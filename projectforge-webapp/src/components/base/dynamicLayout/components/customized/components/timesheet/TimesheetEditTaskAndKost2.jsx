import 'moment/min/locales';
import PropTypes from 'prop-types';
import 'rc-time-picker/assets/index.css';
import React from 'react';
import 'react-day-picker/lib/style.css';
import { Col, FormGroup, Row } from '../../../../../../design';
import { DynamicLayoutContext } from '../../../../context';
import DynamicReactSelect from '../../../select/DynamicReactSelect';
import DynamicTaskSelect from '../../../select/task';

/**
 * Kost2 selection for editing time sheets.
 */
function TimesheetEditTaskAndKost2({ values }) {
    const { ui, variables } = React.useContext(DynamicLayoutContext);

    const [kost2List, setKost2List] = React.useState([]);

    // Only set the kost2list when there is a new value for the variables prop.
    React.useEffect(() => {
        const task = variables ? variables.task : undefined;
        setKost2List(task ? task.kost2List : undefined);
    }, [variables]);

    return React.useMemo(() => {
        let kost2Row;

        if (Array.isArray(kost2List) && kost2List.length) {
            kost2Row = (
                <Row>
                    <Col sm={6}>
                        <FormGroup>
                            <DynamicReactSelect
                                label={ui.translations['fibu.kost2']}
                                id={values.id}
                                values={kost2List}
                                valueProperty="id"
                                labelProperty="title"
                            />
                        </FormGroup>
                    </Col>
                </Row>
            );
        }

        return (
            <React.Fragment>
                <Row>
                    <Col>
                        <DynamicTaskSelect
                            label={ui.translations.task}
                            onKost2Changed={setKost2List}
                            id="task"
                            variables={variables}
                        />
                    </Col>
                </Row>
                {kost2Row}
            </React.Fragment>
        );
    }, [kost2List]);
}

TimesheetEditTaskAndKost2.propTypes = {
    values: PropTypes.shape({}).isRequired,
};

TimesheetEditTaskAndKost2.defaultProps = {};

export default TimesheetEditTaskAndKost2;
