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
    /** Rejected by the dropzone before any transfer started (too large, too many files). */
    errors: FileError[];
    /** Set once the transfer itself failed; already translated and ready to display. */
    uploadError?: string;
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
        const mappedAcc = accFiles.map((file) => ({
            file,
            errors: [],
            id: getNewId(),
        }));

        const mappedRej = rejFiles.map((r) => ({
            file: r.file,
            errors: [...r.errors],
            id: getNewId(),
        }));

        // Keep whatever is still on screen. Dropping the failed entries here (as this did before)
        // meant a rejection or a broken transfer disappeared as soon as the user tried again -
        // exactly when they would want to compare the two attempts.
        setFiles((curr) => [
            ...curr,
            ...mappedAcc,
            ...mappedRej,
        ]);
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

    // A failed transfer keeps its row, now showing why. Removing it (which is what happened before,
    // by way of an unhandled rejection) left the user with a progress bar that simply vanished.
    const onUploadFailed = (file: File, error: string) => {
        setFiles((curr) => curr.map((fw) => (
            fw.file === file ? { ...fw, uploadError: error } : fw
        )));
    };

    const { getRootProps, getInputProps } = useDropzone({
        onDrop,
        maxSize: maxSizeInKB * 1024,
        maxFiles: 20, // Limit to 10 parallels uploads.
    });

    const translateError = (errors: FileError[]) => {
        if (!errors || errors.length === 0) {
            return 'Unknown upload error.';
        }
        const error = errors[0];
        if (error.code === 'file-too-large') {
            return translations['file.upload.error.maxSizeOfExceeded'];
        }
        if (error.code === 'too-many-files') {
            return translations['file.upload.error.tooManyFiles'];
        }
        return error.message;
    };

    return (
        <>
            <div {...getRootProps()} className="dropZone">
                <span className="info">
                    <FontAwesomeIcon icon={faUpload} />
                    {' '}
                    {title}
                </span>
                <input {...getInputProps()} />

            </div>

            {files.map((fileWrapper) => (
                <div key={fileWrapper.id}>
                    {fileWrapper.errors.length || fileWrapper.uploadError ? (
                        <UploadError
                            file={fileWrapper.file}
                            error={fileWrapper.uploadError || translateError(fileWrapper.errors)}
                        />
                    ) : (
                        <SingleFileUploadWithProgress
                            onUpload={onUpload}
                            file={fileWrapper.file}
                            url={url}
                            afterFileUpload={onAfterUpload}
                            onUploadFailed={onUploadFailed}
                            translations={translations}
                        />
                    )}
                </div>
            ))}
        </>
    );
}
