import { faTimesCircle } from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import React from 'react';
import { Button } from '../../../../../design';
import inputStyle from '../../../../../design/input/Input.module.scss';
import taskStyle from './Ta'

function TaskPath({ path, setTask, modalHighlight }) {
    let recentAncecstorId;

    return (
        <React.Fragment>
            {path.map((ancestor) => {
                const parenTaskId = recentAncecstorId;
                let removeLink = (
                    <Button
                        color="link"
                        onClick={() => setTask(parenTaskId)}
                        style={{ padding: 0 }}
                    >
                        <FontAwesomeIcon
                            icon={faTimesCircle}
                            className={inputStyle.icon}
                            color="lightGray"
                        />
                    </Button>
                );

                recentAncecstorId = ancestor.id;

                // TODO: LAST POINT OF EDIT
                return (
                    <React.Fragment key={ancestor.id}>
                        {ancestor.id === modalHighlight
                        ? <span className={}></span>}
                    </React.Fragment>
                )
            })}
        </React.Fragment>
    );
}

TaskPath.propTypes = {};

TaskPath.defaultProps = {};

export default TaskPath;
