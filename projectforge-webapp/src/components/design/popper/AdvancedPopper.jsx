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
        onBlur,
        setIsOpen,
        withInput,
        ...props
    },
) {
    const reference = React.useRef(null);
    const basicReference = React.useRef(null);
    const [basicHeight, setBasicHeight] = React.useState(0);
    const [basicWidth, setBasicWidth] = React.useState(0);
    const [additionalHeight, setAdditionalHeight] = React.useState(0);
    const [additionalWidth, setAdditionalWidth] = React.useState(0);
    const [currentTimeout, setCurrentTimeout] = React.useState(-1);

    useClickOutsideHandler(reference, setIsOpen, isOpen);

    // Clear timeout on unmount
    React.useEffect(() => () => {
        if (currentTimeout >= 0) {
            clearTimeout(currentTimeout);
        }
    }, []);

    React.useLayoutEffect(
        () => {
            if (basicReference.current) {
                setBasicWidth(basicReference.current.clientWidth);
                setBasicHeight(basicReference.current.clientHeight);
            }
        },
        [
            basicReference.current && basicReference.current.clientHeight,
            basicReference.current && basicReference.current.clientWidth,
        ],
    );
    React.useLayoutEffect(
        () => {
            if (reference.current) {
                const { top, left } = reference.current.getBoundingClientRect();

                setAdditionalHeight(document.body.clientHeight - top - 64);
                setAdditionalWidth(document.body.clientWidth - left - 16);
            }
        },
        [
            reference.current && Math.floor(reference.current.getBoundingClientRect().top),
            reference.current && Math.floor(reference.current.getBoundingClientRect().left),
        ],
    );

    const handleBlur = (event) => {
        if (reference.current) {
            if (currentTimeout) {
                clearTimeout(currentTimeout);
            }

            // Get new active element after blur
            setCurrentTimeout(
                setTimeout(() => {
                    if (!reference.current.contains(document.activeElement)) {
                        setIsOpen(false);
                    }
                }, 1),
            );
        }

        if (onBlur) {
            onBlur(event);
        }
    };

    const handleClick = ({ target }) => {
        if (basicReference.current && basicReference.current.contains(target)) {
            setIsOpen(true);
        }
    };

    const additionalVisible = isOpen && children;

    return (
        <div
            ref={reference}
            className={classNames(
                style.advancedPopperContainer,
                { [style.isOpen]: additionalVisible },
                className,
            )}
            {...props}
            onBlur={handleBlur}
            role="menu"
            onClick={handleClick}
            onKeyDown={undefined}
            tabIndex={0}
        >
            <div
                className={classNames(
                    style.content,
                    { [style.noBorder]: withInput },
                    contentClassName,
                )}
                ref={basicReference}
            >
                {basic}
            </div>
            {additionalVisible && (
                <div
                    className={classNames(
                        style.additional,
                        { [style.withInput]: withInput },
                        additionalClassName,
                    )}
                    style={{
                        top: basicHeight + (withInput ? 0 : 10),
                        minWidth: basicWidth,
                        maxWidth: additionalWidth,
                        maxHeight: additionalHeight,
                    }}
                >
                    {children}
                    {actions && (
                        <div className={style.actions}>
                            {actions}
                        </div>
                    )}
                </div>
            )}
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
    onBlur: PropTypes.func,
    withInput: PropTypes.bool,
};

AdvancedPopper.defaultProps = {
    actions: undefined,
    additionalClassName: undefined,
    children: undefined,
    className: undefined,
    contentClassName: undefined,
    isOpen: false,
    onBlur: undefined,
    withInput: false,
};

export default AdvancedPopper;
