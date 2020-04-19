import { faCopy, faEye, faEyeSlash } from '@fortawesome/free-regular-svg-icons';
import { faClipboardCheck } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import copy from 'clipboard-copy';
import PropTypes from 'prop-types';
import React from 'react';
import UncontrolledTooltip from 'reactstrap/lib/UncontrolledTooltip';
import selectNodeText from '../../../utilities/select';
import TooltipIcon from '../TooltipIcon';
import AdditionalLabel from './AdditionalLabel';
import styles from './Input.module.scss';
import InputContainer from './InputContainer';

/**
 * ReadonlyField text (with label and optional tooltip)
 */
function ReadonlyField(
    {
        additionalLabel,
        canCopy,
        coverUp,
        id,
        label,
        tooltip,
        value,
        ...props
    },
) {
    const [showCover, setShowCover] = React.useState(true);
    const [isCopied, setIsCopied] = React.useState(0);
    const valueRef = React.useRef(null);

    const copyValue = () => copy(value)
        .then(() => setIsCopied(1))
        .catch(() => setIsCopied(-1));

    React.useEffect(() => setIsCopied(0), [value]);

    const handleContainerClick = () => {
        if (valueRef.current) {
            selectNodeText(valueRef.current);
        }
    };

    return (
        <React.Fragment>
            <InputContainer
                className={styles.readOnly}
                label={(
                    <React.Fragment>
                        {label}
                        {tooltip && (
                            <React.Fragment>
                                <TooltipIcon />
                                <UncontrolledTooltip placement="auto" target={id}>
                                    {tooltip}
                                </UncontrolledTooltip>
                            </React.Fragment>
                        )}
                    </React.Fragment>
                )}
                id={id}
                isActive
                onClick={handleContainerClick}
                readOnly
                withMargin
                {...props}
            >
                {value && (
                    <React.Fragment>
                        <div className={styles.icons}>
                            {coverUp && (
                                <FontAwesomeIcon
                                    icon={showCover ? faEye : faEyeSlash}
                                    onClick={() => setShowCover(!showCover)}
                                />
                            )}
                            {(coverUp || canCopy) && (
                                <FontAwesomeIcon
                                    className={classNames({
                                        [styles.success]: isCopied === 1,
                                        [styles.error]: isCopied === -1,
                                    })}
                                    icon={isCopied === 1 ? faClipboardCheck : faCopy}
                                    onClick={copyValue}
                                />
                            )}
                        </div>
                        {coverUp && showCover && (
                            <div
                                className={styles.coverUp}
                                style={{ width: `${value.length + 1}ch` }}
                            />
                        )}
                    </React.Fragment>
                )}
                <p className={styles.value}>
                    <span ref={valueRef}>
                        {value && coverUp && showCover
                            ? `${value.substr(0, value.length / 2)}***`
                            : (value || '-')}
                    </span>
                    &nbsp;
                </p>
            </InputContainer>
            <AdditionalLabel title={additionalLabel} />
        </React.Fragment>
    );
}

ReadonlyField.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    canCopy: PropTypes.bool,
    coverUp: PropTypes.bool,
    tooltip: PropTypes.string,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
};

ReadonlyField.defaultProps = {
    additionalLabel: undefined,
    canCopy: false,
    coverUp: false,
    tooltip: undefined,
    value: undefined,
};

export default ReadonlyField;
