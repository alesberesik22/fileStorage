DELETE FROM fileuploadDownload;

ALTER TABLE fileuploadDownload
ADD COLUMN folderPath TEXT NOT NULL;