import { faUpload } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React from 'react';
import style from './DropArea.module.scss';

class DropArea extends React.Component {
    static handleDragOver(event) {
        event.preventDefault();
    }

    static areFilesEqual(file, otherFile) {
        return file.lastModified === otherFile.lastModified
            && file.name === otherFile.name
            && file.size === otherFile.size;
    }

    constructor(props) {
        super(props);

        this.state = {
            inDrag: false,
            files: [],
        };

        this.input = React.createRef();

        this.handleDragEnter = this.handleDragEnter.bind(this);
        this.handleDragLeave = this.handleDragLeave.bind(this);
        this.handleDropCapture = this.handleDropCapture.bind(this);
        this.handleInputChange = this.handleInputChange.bind(this);
        this.addFiles = this.addFiles.bind(this);
    }

    handleDragEnter(event) {
        event.preventDefault();

        this.setState({ inDrag: true });
    }

    handleDragLeave(event) {
        event.preventDefault();

        this.setState({ inDrag: false });
    }

    handleDropCapture(event) {
        this.handleDragLeave(event);

        this.addFiles(event.dataTransfer.files);
    }

    handleInputChange(event) {
        this.addFiles(event.target.files);
    }

    addFiles(fileList) {
        const { multiple, setFiles } = this.props;
        let newFiles;

        if (multiple) {
            const { files } = this.state;

            newFiles = Array.of(
                ...files,
                fileList.filter((file) => !files.find((cf) => DropArea.areFilesEqual(file, cf))),
            );
        } else {
            newFiles = [fileList[0]];
        }

        this.setState({ files: newFiles });

        if (setFiles) {
            setFiles(newFiles);
        }
    }

    render() {
        const {
            children,
            multiple,
            noStyle,
            title,
        } = this.props;
        const { inDrag } = this.state;

        const inputProps = {};

        if (multiple) {
            inputProps.name = 'files[]';
            inputProps['data-multiple-caption'] = '{count} files selected.';
            inputProps.multiple = true;
        }

        return (
            <div
                role="button"
                onKeyDown={() => {
                }}
                onClick={() => this.input.current.click()}
                className={classNames(style.dropArea, {
                    [style.inDrag]: inDrag,
                    [style.noStyle]: noStyle,
                })}
                tabIndex={-1}
            >
                <div
                    onDragEnter={this.handleDragEnter}
                    onDragLeave={this.handleDragLeave}
                    onDragOver={DropArea.handleDragOver}
                    onDropCapture={this.handleDropCapture}
                    className={style.background}
                >
                    <input
                        onChange={this.handleInputChange}
                        type="file"
                        className={style.file}
                        ref={this.input}
                        // eslint-disable-next-line react/jsx-props-no-spreading
                        {...inputProps}
                    />
                    <span className={style.info}>
                        <FontAwesomeIcon icon={faUpload} className={style.icon} />
                        {title}
                    </span>
                    {children}
                </div>
            </div>
        );
    }
}

DropArea.propTypes = {
    children: PropTypes.node,
    multiple: PropTypes.bool,
    noStyle: PropTypes.bool,
    setFiles: PropTypes.func,
    title: PropTypes.string,
};

DropArea.defaultProps = {
    children: undefined,
    multiple: false,
    noStyle: false,
    setFiles: undefined,
    title: 'Select a file, or drop one here.',
};

export default DropArea;
