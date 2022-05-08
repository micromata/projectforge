import React, { useCallback, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUpload } from '@fortawesome/free-solid-svg-icons';
import { FileError, FileRejection, useDropzone } from 'react-dropzone';
import { SingleFileUploadWithProgress } from './SingleFileUploadWithProgress';
import { UploadError } from './UploadError';

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

export function MultipleFileUploadArea({ url, title, afterFileUpload }: { url: string, title: string, afterFileUpload: (response: string) => void }) {
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

    const onDelete = (file: File) => {
        setFiles((curr) => curr.filter((fw) => fw.file !== file));
    };

    const { getRootProps, getInputProps } = useDropzone({
        onDrop,
        maxSize: 300 * 1024 * 1024, // 300MB
    });

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
                            errors={fileWrapper.errors}
                            onDelete={onDelete}
                        />
                    ) : (
                        <SingleFileUploadWithProgress
                            onDelete={onDelete}
                            onUpload={onUpload}
                            file={fileWrapper.file}
                            url={url}
                            afterFileUpload={afterFileUpload}
                        />
                    )}
                </div>
            ))}
        </>
    );
}
