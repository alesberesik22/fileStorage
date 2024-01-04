CREATE TABLE fileuploadDownload (
    fileId UUID PRIMARY KEY,
    fileName TEXT NOT NULL,
    fileType TEXT NOT NULL,
    data BYTEA NOT NULL
);