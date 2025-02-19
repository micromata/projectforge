import PropTypes from 'prop-types';
import React, { useContext, useEffect, useMemo, useRef } from 'react';
import { Progress } from 'reactstrap';
import { colorPropType } from '../../../../utilities/propTypes';
import { DynamicLayoutContext } from '../context';
import { fetchJsonGet } from '../../../../utilities/rest';
import DynamicButton from './DynamicButton';
import DynamicAlert from './DynamicAlert';

function DynamicProgress(props) {
    const {
        title,
        color,
        id,
        value = 0,
        info,
        infoColor,
        cancelId,
        cancelConfirmMessage,
        onCancelClick,
        animated,
    } = props;
    const { data, variables, ui } = useContext(DynamicLayoutContext);
    const handleCancelClick = () => {
        onCancelClick(cancelId);
    };

    let state;
    if (id) {
        state = Object.getByString(data, id) || Object.getByString(variables, id);
    }
    if (!state) {
        state = {
            value, title, color, info, animated, infoColor, cancelId,
        };
    }
    const {
        value: useValue, title: useTitle, color: useColor, animated: useAnimated,
        cancelId: useCancelId, info: useInfo, infoColor: useInfoColor,
    } = state;

    let cancelButton;
    if (useCancelId) {
        cancelButton = (
            <DynamicButton
                id="next"
                title={ui.translations.cancel || 'Cancel'}
                color="danger"
                outline
                handleButtonClick={handleCancelClick}
                confirmMessage={cancelConfirmMessage}
            />
        );
    }
    return (
        <>
            <div>
                {useTitle}
            </div>
            <div className="job-progress">
                <Progress
                    className="job-progress"
                    value={useValue}
                    color={useColor}
                    animated={useAnimated}
                >
                    {`${useValue}%`}
                </Progress>
                {cancelButton}
            </div>
            {useInfo && (
                <DynamicAlert color={useColor} message={useInfo} markdown />
            )}
        </>
    );
}

DynamicProgress.propTypes = {
    title: PropTypes.string,
    color: colorPropType,
    value: PropTypes.number,
    info: PropTypes.string,
    infoColor: PropTypes.string,
    id: PropTypes.string,
    cancelId: PropTypes.number || PropTypes.string,
    cancelConfirmMessage: PropTypes.string,
    onCancelClick: PropTypes.func,
    animated: PropTypes.bool,
};

export default DynamicProgress;
