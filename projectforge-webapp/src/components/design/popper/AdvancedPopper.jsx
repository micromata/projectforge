import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import { useClickOutsideHandler } from '../../../utilities/hooks';
import style from './Popper.module.scss';

function AdvancedPopper(
    {
        actions,
        additionalClassName,
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
    const [additionalHeight, setAdditionalHeight] = React.useState(0);

    useClickOutsideHandler(reference, setIsOpen, isOpen);

    React.useLayoutEffect(
        () => {
            if (reference.current) {
                setBasicWidth(basicReference.current.clientWidth);
                setAdditionalHeight(
                    window.innerHeight
                    - reference.current.getBoundingClientRect().top
                    - basicHeight
                    - 64,
                );
            }
            if (basicReference.current) {
                setBasicHeight(basicReference.current.clientHeight);
            }
        },
        [
            basicReference.current && basicReference.current.clientHeight,
            reference.current && reference.current.clientWidth,
            reference.current && reference.current.getBoundingClientRect().top,
        ],
    );

    const additionalVisible = isOpen && children;

    console.log('Hey');

    return (
        <div
            ref={reference}
            className={classNames(
                style.advancedPopperContainer,
                { [style.isOpen]: additionalVisible },
                className,
            )}
        >
            <div
                className={classNames(style.content, contentClassName)}
                role="menu"
                ref={basicReference}
                tabIndex={0}
                onClick={() => setIsOpen(true)}
                onKeyDown={() => setIsOpen(true)}
            >
                {basic}
            </div>
            <div
                className={classNames(style.additional, additionalClassName)}
                style={{
                    top: basicHeight + 10,
                    minWidth: basicWidth + 2,
                    maxHeight: additionalVisible ? additionalHeight : 0,
                }}
            >
                {children}
                {actions && (
                    <div className={style.actions}>
                        {actions}
                    </div>
                )}
            </div>
        </div>
    );
}

AdvancedPopper.propTypes = {
    basic: PropTypes.node.isRequired,
    setIsOpen: PropTypes.func.isRequired,
    actions: PropTypes.node,
    additionalClassName: PropTypes.string,
    children: PropTypes.node,
    className: PropTypes.string,
    contentClassName: PropTypes.string,
    isOpen: PropTypes.bool,
};

AdvancedPopper.defaultProps = {
    actions: undefined,
    additionalClassName: undefined,
    children: undefined,
    className: undefined,
    contentClassName: undefined,
    isOpen: false,
};

export default AdvancedPopper;
