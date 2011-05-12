-- record of user upload files
create table UPLOAD_FILE (
    -- use the File.canonicalPath as the primary key
    PATH varchar2(4000),
    -- owner of the file
    USER_ID varchar2 (255) not null,
    NAME varchar2 (255),
    EXTENSION varchar2 (16),
    KIND varchar2 (32), -- from SemanticUtil.getKind, usually the extension
    FILE_LENGTH NUMBER(20,0), -- the length in bytes of the file
    LAST_MODIFIED timestamp, -- the last modified date of the file
constraint uf_pk primary key (PATH),
constraint uf_fk foreign key (USER_ID) references GP_USER(USER_ID)
);

CREATE INDEX IDX_UF_USER_ID ON UPLOAD_FILE (USER_ID);

-- update schema version
INSERT INTO PROPS (KEY, VALUE) VALUES ('registeredVersion3.3.2', '3.3.2');

COMMIT;









