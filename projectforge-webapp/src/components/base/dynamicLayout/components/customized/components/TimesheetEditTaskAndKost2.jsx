import 'moment/min/locales';
import PropTypes from 'prop-types';
import 'rc-time-picker/assets/index.css';
import React from 'react';
import 'react-day-picker/lib/style.css';
import { Col, FormGroup, Row } from '../../../../../design';
import { DynamicLayoutContext } from '../../../context';
import DynamicReactSelect from '../../select/DynamicReactSelect';
import DynamicTaskSelect from '../../select/task';

/**
 * Kost2 selection for editing time sheets.
 */
function TimesheetEditTaskAndKost2({ values, variables }) {
    const { ui } = React.useContext(DynamicLayoutContext);

    const [kost2List, setKost2List] = React.useState(undefined);

    // Only set the kost2list when there is a new value for the variables prop.
    React.useEffect(() => {
        const task = variables ? variables.task : undefined;
        setKost2List(task ? task.kost2List : undefined);
    }, [variables]);

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
                    <FormGroup>
                        <DynamicTaskSelect
                            label={ui.translations.task}
                            onKost2Changed={setKost2List}
                            id="task"
                            variables={variables}
                        />
                    </FormGroup>
                </Col>
            </Row>
            {kost2Row}
        </React.Fragment>
    );
}

TimesheetEditTaskAndKost2.propTypes = {
    values: PropTypes.shape({}).isRequired,
    variables: PropTypes.shape({}).isRequired,
};

TimesheetEditTaskAndKost2.defaultProps = {};

export default TimesheetEditTaskAndKost2;
