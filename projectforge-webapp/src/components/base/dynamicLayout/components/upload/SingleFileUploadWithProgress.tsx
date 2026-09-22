import React, { useEffect, useState } from 'react';
import { Progress } from 'reactstrap';
import { FileHeader } from './FileHeader';

/* eslint-disable max-len */

export interface SingleFileUploadWithProgressProps {
    file: File;
    url: string;
    onUpload: (file: File, url: string) => void;
    afterFileUpload: (file: File, response: string) => void;
    /** Called when the file never made it to the server, with a message ready to display. */
    onUploadFailed: (file: File, error: string) => void;
    /** Translated texts, keyed as in I18nResources: see UIAttachmentList.addTranslations. */
    translations: { [key: string]: string };
}

/** Thrown for anything that kept the response from being a successful upload. */
class UploadFailed extends Error {
}

/**
 * POSTs the file and reports progress while it goes out.
 *
 * XHR rather than fetch, because `XMLHttpRequest.upload.onprogress` is the only portable way to
 * count the bytes of a request body — fetch has no upload-progress event.
 *
 * Rejects for every outcome that is not a 2xx, the aborted transfer included. Getting this wrong is
 * what made large uploads fail invisibly: a proxy that refuses the body (413) or drops the
 * connection answers with an HTML page or with nothing at all, so parsing the body unconditionally
 * threw *inside* the event handler, where the rejection went nowhere.
 */
function uploadFile(
    file: File,
    url: string,
    onProgress: (percentage: number) => void,
    translations: { [key: string]: string },
) {
    const failedText = translations['file.upload.error'] || 'Error while uploading the file.';
    // The technical cause is appended deliberately: without it a failing upload gives neither the
    // user nor the log anything to report, which is exactly how this class of bug stays invisible.
    const failed = (detail: string) => new UploadFailed(`${failedText} (${detail})`);

    return new Promise<string>((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', url);
        xhr.withCredentials = true;

        xhr.upload.onprogress = (event) => {
            if (event.lengthComputable) {
                const percentage = (event.loaded / event.total) * 100;
                onProgress(Math.round(percentage));
            }
        };
        xhr.onload = () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                resolve(xhr.responseText);
            } else {
                // A status of 0 here means the request was answered but the body could not be read.
                reject(failed(`HTTP ${xhr.status}`));
            }
        };
        // XHR withholds the cause of a network error (it would leak cross-origin information), so
        // the kind of event is all there is to report. This is the path a connection aborted
        // mid-body takes — a proxy timeout or body limit anywhere in the chain.
        xhr.onerror = () => reject(failed('network'));
        xhr.ontimeout = () => reject(failed('timeout'));
        xhr.onabort = () => reject(failed('aborted'));

        const formData = new FormData();
        formData.append('file', file);

        xhr.send(formData);
    });
}

export function SingleFileUploadWithProgress({
    file,
    url,
    onUpload,
    afterFileUpload,
    onUploadFailed,
    translations,
}: SingleFileUploadWithProgressProps) {
    const [progress, setProgress] = useState(0);

    let color = 'warning';
    let animated = true;
    if (progress === 100) {
        color = 'success';
        animated = false;
    }

    useEffect(() => {
        async function upload() {
            try {
                const response = await uploadFile(file, url, setProgress, translations);
                // The single place the answer is handed on. A second reporting path (the former
                // onreadystatechange) parsed the same body again and swallowed the same errors.
                afterFileUpload(file, response);
                onUpload(file, url);
            } catch (error) {
                onUploadFailed(file, error instanceof Error ? error.message : String(error));
            }
        }

        upload();
    }, []);

    return (
        <div className="uploadProgress">
            <FileHeader file={file} />
            <Progress
                animated={animated}
                color={color}
                value={progress}
            >
                {progress}
                {' %'}
            </Progress>
        </div>
    );
}
