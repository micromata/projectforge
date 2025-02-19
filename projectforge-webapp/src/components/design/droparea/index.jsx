import { faUpload } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React, { useRef, useState } from 'react';
import style from './DropArea.module.scss';

const handleDragOver = (event) => {
    event.preventDefault();
};

const areFilesEqual = (file, otherFile) => file.lastModified === otherFile.lastModified
        && file.name === otherFile.name
        && file.size === otherFile.size;

function DropArea({
    children,
    multiple = false,
    noStyle = false,
    setFiles,
    title = 'Select a file, or drop one here.',
    id,
}) {
    const [inDrag, setInDrag] = useState(false);
    const [fileList, setFileList] = useState([]);
    const input = useRef(null);

    const handleDragEnter = (event) => {
        event.preventDefault();
        setInDrag(true);
    };

    const handleDragLeave = (event) => {
        event.preventDefault();
        setInDrag(false);
    };

    const addFiles = (newFileList) => {
        let newFiles;

        if (multiple) {
            newFiles = [
                ...fileList,
                ...Array.from(newFileList).filter(
                    (file) => !fileList.find((cf) => areFilesEqual(file, cf)),
                ),
            ];
        } else {
            newFiles = [newFileList[0]];
        }

        setFileList(newFiles);

        if (setFiles) {
            setFiles(newFiles);
        }
    };

    const handleDropCapture = (event) => {
        handleDragLeave(event);
        addFiles(event.dataTransfer.files);
    };

    const handleInputChange = (event) => {
        addFiles(event.target.files);
    };

    const inputProps = {
        ...(multiple && {
            name: 'files[]',
            'data-multiple-caption': '{count} files selected.',
            multiple: true,
        }),
    };

    return (
        <div
            id={id}
            role="button"
            onKeyDown={() => undefined}
            onClick={() => input.current.click()}
            className={classNames(style.dropArea, {
                [style.inDrag]: inDrag,
                [style.noStyle]: noStyle,
            })}
            tabIndex={-1}
        >
            <div
                onDragEnter={handleDragEnter}
                onDragLeave={handleDragLeave}
                onDragOver={handleDragOver}
                onDropCapture={handleDropCapture}
                className={style.background}
            >
                <input
                    onChange={handleInputChange}
                    type="file"
                    className={style.file}
                    ref={input}
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

DropArea.propTypes = {
    children: PropTypes.node,
    multiple: PropTypes.bool,
    noStyle: PropTypes.bool,
    setFiles: PropTypes.func,
    title: PropTypes.string,
    id: PropTypes.string,
};

export default DropArea;
