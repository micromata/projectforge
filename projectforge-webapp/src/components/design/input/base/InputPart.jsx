import PropTypes from 'prop-types';
import React, { Component } from 'react';
import style from './Base.module.scss';
import BasePart from './Part';

class InputPart extends Component {
    constructor(props) {
        super(props);

        this.inputRef = React.createRef();
    }

    componentDidUpdate({ selectionStart: prevSelectionStart }) {
        const { selectionStart } = this.props;

        if (prevSelectionStart !== selectionStart && this.inputRef.current) {
            this.inputRef.current.setSelectionRange(selectionStart, selectionStart);
            this.inputRef.current.focus();
        }
    }

    render() {
        const { flexSize, selectionStart, ...props } = this.props;

        return (
            <BasePart flexSize={flexSize}>
                <input
                    type="text"
                    className={style.input}
                    {...props}
                    ref={this.inputRef}
                />
            </BasePart>
        );
    }
}

InputPart.propTypes = {
    id: PropTypes.string.isRequired,
    flexSize: PropTypes.number,
    onFocus: PropTypes.func,
    selectionStart: PropTypes.number,
};

InputPart.defaultProps = {
    flexSize: 1,
    onFocus: undefined,
    selectionStart: undefined,
};

export default InputPart;
