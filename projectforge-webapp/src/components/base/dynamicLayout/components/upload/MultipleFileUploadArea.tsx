import React, { useCallback, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUpload } from '@fortawesome/free-solid-svg-icons';
import { FileError, FileRejection, useDropzone } from 'react-dropzone';
import { SingleFileUploadWithProgress } from './SingleFileUploadWithProgress';
import { UploadError } from './UploadError';
import { DynamicLayoutContext } from '../../context';

/* eslint-disable max-len */

/*
 * Thanks to: https://github.com/bmvantunes/youtube-2021-feb-multiple-file-upload-formik/blob/main/src/upload/MultipleFileUploadField.tsx
 */

let currentId = 0;

function getNewId() {
    // we could use a fancier solution instead of a sequential ID :)
    currentId += 1;
    return currentId;
}

export interface UploadableFile {
    // id was added after the video being released to fix a bug
    // Video with the bug -> https://youtube-2021-feb-multiple-file-upload-formik-bmvantunes.vercel.app/bug-report-SMC-Alpha-thank-you.mov
    // Thank you for the bug report SMC Alpha - https://www.youtube.com/channel/UC9C4AlREWdLoKbiLNiZ7XEA
    id: number;
    file: File;
    errors: FileError[];
}

/*
 Prepared for existingFiles
export interface UploadedFile {
    fileId: string;
    name: string;
    size: number;
} */

export function MultipleFileUploadArea(
    {
        url,
        title,
        afterFileUpload,
        maxSizeInKB,
        // existingFiles,
    }:
        {
            url: string,
            title: string,
            afterFileUpload: (response: string) => void,
            maxSizeInKB: number,
            // existingFiles: UploadedFile[],
        },
) {
    const { ui } = React.useContext(DynamicLayoutContext);
    const { translations } = ui;

    // const [_, __, helpers] = useField(name);
    // const classes = useStyles();
    const [files, setFiles] = useState<UploadableFile[]>([]);
    const onDrop = useCallback((accFiles: File[], rejFiles: FileRejection[]) => {
        const mappedAcc = accFiles.map((file) => ({ file, errors: [], id: getNewId() }));
        const mappedRej = rejFiles.map((r) => ({ ...r, id: getNewId() }));
        setFiles((curr) => [...curr, ...mappedAcc, ...mappedRej]);
    }, []);

    /* useEffect(() => {
        helpers.setValue(files);
        // helpers.setTouched(true);
    }, [files]); */

    const onUpload = (file: File) => {
        setFiles((curr) => curr.map((fw) => {
            if (fw.file === file) {
                return { ...fw, url };
            }
            return fw;
        }));
    };

    /*
    const validator = (file: File) => {
        // Return FileError
        existingFiles.map((existingFile) => {
            if (file.name === existingFile.name) {
                return {
                    message: 'File already exists.',
                    code: 'FileAlreadyEsists',
                };
            }
            return undefined;
        });
    }; */

    const onAfterUpload = (file: File, response: string) => {
        setFiles((curr) => curr.filter((fw) => fw.file !== file));
        afterFileUpload(response);
    };

    const { getRootProps, getInputProps } = useDropzone({
        onDrop,
        maxSize: maxSizeInKB * 1024,
        maxFiles: 10, // Limit to 10 parallels uploads.
    });

    const translateError = (errors: FileError[]) => {
        if (!errors || errors.length === 0) {
            return 'Unknown upload error.';
        }
        const error = errors[0];
        if (error.code === 'file-too-large') {
            return translations['file.upload.maxSizeOfExceeded'];
        }
        if (error.code === 'too-many-files') {
            return translations['file.upload.toManyFiles'];
        }
        return error.message;
    };

    return (
        <>
            <div {...getRootProps()}>
                <p>
                    <FontAwesomeIcon icon={faUpload} />
                    {' '}
                    {title}
                </p>
                <input {...getInputProps()} />

            </div>

            {files.map((fileWrapper) => (
                <div key={fileWrapper.id}>
                    {fileWrapper.errors.length ? (
                        <UploadError
                            file={fileWrapper.file}
                            error={translateError(fileWrapper.errors)}
                        />
                    ) : (
                        <SingleFileUploadWithProgress
                            onUpload={onUpload}
                            file={fileWrapper.file}
                            url={url}
                            afterFileUpload={onAfterUpload}
                        />
                    )}
                </div>
            ))}
        </>
    );
}
