import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { useClickOutsideHandler } from '../../../utilities/hooks';
import style from './Popper.module.scss';

function AdvancedPopper(
    {
        basic,
        children,
        className,
        contentClassName,
        isOpen,
        setIsOpen,
    },
) {
    const reference = React.useRef(null);
    const basicReference = React.useRef(null);
    const [basicHeight, setBasicHeight] = React.useState(0);
    const [basicWidth, setBasicWidth] = React.useState(0);

    useClickOutsideHandler(reference, setIsOpen, isOpen);

    React.useLayoutEffect(
        () => {
            setBasicHeight(basicReference.current.clientHeight);
            setBasicWidth(basicReference.current.clientWidth);
        },
        [
            basicReference.current && basicReference.current.clientHeight,
            basicReference.current && basicReference.current.clientWidth,
        ],
    );

    return (
        <div
            className={classNames(
                style.advancedPopperContainer,
                { [style.isOpen]: isOpen },
                className,
            )}
        >
            <div
                style={{
                    height: `${basicHeight}px`,
                    width: `${basicWidth}px`,
                }}
            />
            <div
                ref={reference}
                className={classNames(style.content, contentClassName)}
            >
                <div ref={basicReference} onFocus={() => setIsOpen(true)}>
                    {basic}
                </div>
                <div className={style.additional}>
                    {children}
                </div>
            </div>
        </div>
    );
}

AdvancedPopper.propTypes = {
    basic: PropTypes.node.isRequired,
    children: PropTypes.node.isRequired,
    setIsOpen: PropTypes.func.isRequired,
    className: PropTypes.string,
    contentClassName: PropTypes.string,
    isOpen: PropTypes.bool,
};

AdvancedPopper.defaultProps = {
    className: undefined,
    contentClassName: undefined,
    isOpen: false,
};

export default AdvancedPopper;
