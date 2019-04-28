import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import { Manager, Popper as PopperJs, Reference } from 'react-popper';
import style from './Popper.module.scss';

function Popper(
    {
        className,
        children,
        direction,
        isOpen,
        target,
        ...props
    },
) {
    return (
        <Manager>
            <Reference>
                {({ ref }) => <div ref={ref}>{target}</div>}
            </Reference>
            {isOpen
                ? ReactDOM.createPortal(
                    <PopperJs placement={direction}>
                        {(
                            {
                                ref,
                                style: popperStyle,
                                placement,
                                arrowProps,
                            },
                        ) => (
                            <div
                                ref={ref}
                                style={popperStyle}
                                data-placement={placement}
                                className={classNames(style.popper, className)}
                                {...props}
                            >
                                {children}
                                <div ref={arrowProps.ref} style={arrowProps.style} />
                            </div>
                        )}
                    </PopperJs>,
                    document.body,
                )
                : undefined}
        </Manager>
    );
}

Popper.propTypes = {
    children: PropTypes.node.isRequired,
    target: PropTypes.node.isRequired,
    className: PropTypes.string,
    direction: PropTypes.string,
    isOpen: PropTypes.bool,
};

Popper.defaultProps = {
    className: undefined,
    direction: 'auto',
    isOpen: false,
};

export default Popper;
