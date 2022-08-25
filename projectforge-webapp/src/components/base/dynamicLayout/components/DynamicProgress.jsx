import PropTypes from 'prop-types';
import React, { useContext, useEffect, useMemo, useRef } from 'react';
import { Progress } from 'reactstrap';
import { colorPropType } from '../../../../utilities/propTypes';
import { DynamicLayoutContext } from '../context';
import { fetchJsonGet } from '../../../../utilities/rest';
import DynamicButton from './DynamicButton';

function DynamicProgress(props) {
    const {
        title,
        color,
        id,
        value,
        info,
        infoColor,
        fetchUpdateUrl,
        fetchUpdateInterval,
        cancelUrl,
        cancelConfirmMessage,
    } = props;
    const {
        data, setData, variables, setVariables, callAction,
        ui,
    } = useContext(DynamicLayoutContext);
    const interval = useRef();
    const fetchUpdateUrlRef = useRef(fetchUpdateUrl);
    const fetchUpdateIntervalRef = useRef(fetchUpdateInterval);
    const cancelUrlRef = useRef(cancelUrl);

    const state = Object.getByString(data, id) || Object.getByString(variables, id)
        || {
            value, title, color, info, infoColor,
        };

    const fetchUpdate = () => {
        fetchJsonGet(
            fetchUpdateUrlRef.current,
            undefined,
            (json) => callAction({ responseAction: json }),
        );
    };

    const handleCancelClick = () => {
        fetchJsonGet(
            cancelUrlRef.current,
            undefined,
            (json) => callAction({ responseAction: json }),
        );
    };

    const {
        value: useValue, title: useTitle, color: useColor, animated: useAnimated,
        showCancelButton, info: useInfo, infoColor: useInfoColor,
    } = state;

    useEffect(() => {
        if (fetchUpdateUrl) {
            interval.current = setInterval(
                () => fetchUpdate(),
                fetchUpdateIntervalRef.current || 1000,
            );
        }
        return () => {
            if (fetchUpdateUrl) {
                clearInterval(interval.current);
            }
        };
    }, []);

    useEffect(() => {
        fetchUpdateUrlRef.current = fetchUpdateUrl;
        fetchUpdateIntervalRef.current = fetchUpdateInterval;
        cancelUrlRef.current = cancelUrl;
    }, [fetchUpdateUrl, fetchUpdateInterval, cancelUrl]);

    let cancelButton;
    if (cancelUrlRef.current && showCancelButton === true) {
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

    return useMemo(
        () => (
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
                {info && (
                    <div>
                        Info
                    </div>
                )}
            </>
        ),
        [state, useValue, useColor, useAnimated, useTitle, setData, variables, setVariables],
    );
}

DynamicProgress.propTypes = {
    title: PropTypes.string,
    color: colorPropType,
    value: PropTypes.number,
    info: PropTypes.string,
    infoColor: PropTypes.string,
    id: PropTypes.string,
    fetchUpdateUrl: PropTypes.string,
    fetchUpdateInterval: PropTypes.number,
    cancelUrl: PropTypes.string,
    cancelConfirmMessage: PropTypes.string,
};

DynamicProgress.defaultProps = {
    title: undefined,
    id: undefined,
    color: undefined,
    value: 0,
    info: undefined,
    infoColor: undefined,
    fetchUpdateUrl: undefined,
    fetchUpdateInterval: undefined,
    cancelUrl: undefined,
    cancelConfirmMessage: undefined,
};

export default DynamicProgress;
