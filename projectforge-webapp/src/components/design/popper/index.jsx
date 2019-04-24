import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import ReactDOM from 'react-dom';
import { Manager, Popper as PopperJs, Reference } from 'react-popper';
import style from './Popper.module.scss';

// Notice: Not Ready yet, use Popovers from react-strap instead.
function Popper(
    {
        className,
        children,
        direction,
        target,
    },
) {
    return (
        <Manager>
            <Reference>
                {({ ref }) => <div ref={ref}>{target}</div>}
            </Reference>
            {ReactDOM.createPortal(
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
                        >
                            {children}
                            <div ref={arrowProps.ref} style={arrowProps.style} />
                        </div>
                    )}
                </PopperJs>,
                // TODO ADD DYNAMIC ELEMENT
                document.getElementById('test'),
            )}
        </Manager>
    );
}

Popper.propTypes = {
    children: PropTypes.node.isRequired,
    target: PropTypes.node.isRequired,
    className: PropTypes.string,
    direction: PropTypes.string,
};

Popper.defaultProps = {
    className: undefined,
    direction: 'auto',
};

export default Popper;
