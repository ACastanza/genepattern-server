-- record of user upload files
create table GS_ACCOUNT (
    -- use the File.canonicalPath as the primary key
    GP_USERID varchar primary key,
    -- owner of the file
    TOKEN varchar (255),
constraint gsa_fk foreign key (GP_USERID) references GP_USER(USER_ID)
);

-- improve performance by creating indexes on the ANALYSIS_JOB table
CREATE INDEX IDX_AJ_STATUS ON ANALYSIS_JOB(STATUS_ID);
CREATE INDEX IDX_AJ_PARENT ON ANALYSIS_JOB(PARENT);

-- from sge_schema_oracle.sql
CREATE TABLE JOB_SGE
(
    GP_JOB_NO INTEGER NOT NULL,
    SGE_JOB_ID VARCHAR(4000),
    SGE_SUBMIT_TIME TIMESTAMP,
    SGE_START_TIME TIMESTAMP,
    SGE_END_TIME TIMESTAMP,
    SGE_RETURN_CODE INTEGER,
    SGE_JOB_COMPLETION_STATUS VARCHAR(4000)
);

CREATE INDEX IDX_JOB_SGE_GP_JOB_NO ON JOB_SGE (GP_JOB_NO);
CREATE INDEX IDX_SGE_JOB_ID on JOB_SGE (SGE_JOB_ID);

-- update schema version
update props set value='3.3.3' where key='schemaVersion';
