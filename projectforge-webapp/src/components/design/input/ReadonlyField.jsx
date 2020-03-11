import { faCopy, faEye, faEyeSlash } from '@fortawesome/free-regular-svg-icons';
import { faClipboardCheck } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import copy from 'clipboard-copy';
import PropTypes from 'prop-types';
import React from 'react';
import UncontrolledTooltip from 'reactstrap/lib/UncontrolledTooltip';
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

    const copyValue = () => copy(value)
        .then(() => setIsCopied(1))
        .catch(() => setIsCopied(-1));

    React.useEffect(() => setIsCopied(0), [value]);

    return (
        <React.Fragment>
            <InputContainer
                className={styles.readOnly}
                label={label}
                isActive={Boolean(value)}
                readOnly
                withMargin
                {...props}
            >
                {coverUp && (
                    <React.Fragment>
                        <div className={styles.icons}>
                            <FontAwesomeIcon
                                icon={showCover ? faEye : faEyeSlash}
                                onClick={() => setShowCover(!showCover)}
                            />
                            <FontAwesomeIcon
                                className={classNames({
                                    [styles.success]: isCopied === 1,
                                    [styles.error]: isCopied === -1,
                                })}
                                icon={isCopied === 1 ? faClipboardCheck : faCopy}
                                onClick={copyValue}
                            />
                        </div>
                        {showCover && (
                            <div
                                className={styles.coverUp}
                                style={{ width: `${value.length}em` }}
                            />
                        )}
                    </React.Fragment>
                )}
                <p className={styles.value}>{value}</p>
                {tooltip && (
                    <React.Fragment>
                        <TooltipIcon />
                        <UncontrolledTooltip placement="auto" target={id}>
                            {tooltip}
                        </UncontrolledTooltip>
                    </React.Fragment>
                )}
            </InputContainer>
            <AdditionalLabel title={additionalLabel} />
        </React.Fragment>
    );
}

ReadonlyField.propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    additionalLabel: PropTypes.string,
    coverUp: PropTypes.bool,
    tooltip: PropTypes.string,
    value: PropTypes.string,
};

ReadonlyField.defaultProps = {
    additionalLabel: undefined,
    coverUp: false,
    tooltip: undefined,
    value: undefined,
};

export default ReadonlyField;
