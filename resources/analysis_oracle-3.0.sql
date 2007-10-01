
/* JOB STATUS CODE TABLE */

CREATE TABLE JOB_STATUS
(
  STATUS_ID NUMBER(10,0) NOT NULL,
  STATUS_NAME VARCHAR2(20 CHAR),
  PRIMARY KEY (STATUS_ID)
);


/* TASK ACCESS CODE TABLE */

CREATE TABLE TASK_ACCESS
(
  ACCESS_ID    NUMBER(10,0) NOT NULL,
  NAME         VARCHAR2(4000),
  DESCRIPTION  VARCHAR2(4000),
  PRIMARY KEY (ACCESS_ID)
);


/* TASK MASTER TABLE */

create table TASK_MASTER (
  TASK_ID number(10,0),
  TASK_NAME varchar2(4000 char),
  DESCRIPTION varchar2(4000 char),
  TYPE_ID number(10,0),
  USER_ID varchar2(4000 char),
  ACCESS_ID number(10,0),
  LSID varchar2(4000 char),
  ISINDEXED NUMBER(1,0) DEFAULT 0 NOT NULL,
  TASKINFOATTRIBUTES CLOB,
  PARAMETER_INFO CLOB,
  PRIMARY KEY(TASK_ID)
 );

CREATE INDEX IDX_TASK_MASTER_USER_ID ON TASK_MASTER (USER_ID);
CREATE INDEX IDX_TASK_MASTER_ACCESS_ID ON TASK_MASTER (ACCESS_ID);
CREATE INDEX IDX_TASK_MASTER_TASK_NAME ON TASK_MASTER (TASK_NAME);
CREATE INDEX IDX_TASK_MASTER_LSID ON TASK_MASTER (LSID);


CREATE SEQUENCE TASK_MASTER_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;




/* ANALYSIS JOB */

CREATE TABLE ANALYSIS_JOB
(
  JOB_NO          NUMBER(10,0) NOT NULL,
  TASK_ID         NUMBER(10,0),
  STATUS_ID       NUMBER(10,0),
  DATE_SUBMITTED  TIMESTAMP,
  DATE_COMPLETED  TIMESTAMP,
  USER_ID         VARCHAR2(4000),
  ISINDEXED       NUMBER(1,0) DEFAULT 0 NOT NULL,
  ACCESS_ID       NUMBER(10,0),
  JOB_NAME        VARCHAR2(4000),
  LSID            VARCHAR2(4000),
  TASK_LSID       VARCHAR2(4000),
  TASK_NAME       VARCHAR2(4000),
  PARENT          NUMBER(10,0),
  DELETED         NUMBER(1,0) DEFAULT 0 NOT NULL,
  PARAMETER_INFO  CLOB,
  PRIMARY KEY (JOB_NO)
);

CREATE SEQUENCE ANALYSIS_JOB_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;



/* SUITEs  */
CREATE TABLE SUITE
(
  LSID VARCHAR2(4000 CHAR),
  NAME VARCHAR2(255 CHAR),
  AUTHOR VARCHAR2(255 CHAR),
  OWNER VARCHAR2(255 CHAR),
  DESCRIPTION VARCHAR2(4000 CHAR),
  ACCESS_ID NUMBER(10,0),
  USER_ID VARCHAR2(255 CHAR),
  PRIMARY KEY (LSID )
);

CREATE TABLE SUITE_MODULES
(
  LSID VARCHAR2(255 CHAR),
  MODULE_LSID VARCHAR2(255 CHAR)
);



/* LSIDS */
CREATE TABLE LSIDS
(
  LSID VARCHAR2(255 CHAR) NOT NULL,
  LSID_NO_VERSION VARCHAR2(255 CHAR),
  LSID_VERSION VARCHAR2(255 CHAR)
);

CREATE INDEX IDX_LSID_VERSION ON LSIDS (LSID_VERSION);

CREATE INDEX IDX_LSID_NO_VERSION ON LSIDS (LSID_NO_VERSION);



/* SEQUENCE TABLES FOR LSIDS */

CREATE TABLE SEQUENCE_TABLE
(
  ID NUMBER(10,0) NOT NULL,
  NAME VARCHAR2(100 CHAR) NOT NULL UNIQUE,
  NEXT_VALUE NUMBER(10,0) NOT NULL,
  PRIMARY KEY (ID)
);


/* PROPERTY TABLE */
CREATE TABLE PROPS
(
  KEY VARCHAR2(100 CHAR) NOT NULL,
  VALUE VARCHAR2(256 CHAR),
  PRIMARY KEY (KEY)
);


/* USER TABLE */

CREATE TABLE GP_USER
(
  USER_ID            VARCHAR2(255 CHAR),
  GP_PASSWORD        RAW(255),
  EMAIL              VARCHAR2(255 CHAR),
  LAST_LOGIN_DATE    TIMESTAMP,
  REGISTRATION_DATE    TIMESTAMP,
  LAST_LOGIN_IP      VARCHAR2(255 BYTE),
  TOTAL_LOGIN_COUNT  INTEGER  DEFAULT 0  NOT NULL,
  PRIMARY KEY (USER_ID)
);


CREATE TABLE GP_USER_PROP
(
  ID NUMBER(10) NOT NULL,
  KEY VARCHAR2(255 CHAR),
  VALUE VARCHAR2(255 CHAR),
  GP_USER_ID VARCHAR2(255 CHAR) NOT NULL,
  PRIMARY KEY (ID)
);
CREATE INDEX IDX_GP_USER_PROP_KEY ON GP_USER_PROP (KEY);

CREATE SEQUENCE GP_USER_PROP_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;



/* Event logging */
create table job_completion_event
(
  ID NUMBER(10) NOT NULL,
  user_id varchar2(255),
  type varchar2(255),
  job_number number(10),
  parent_job_number number(10),
  task_lsid varchar2(255),
  task_name varchar2(255),
  completion_status varchar2(255),
  completion_date timestamp,
  elapsed_time number(10),
  primary key (id)
);

CREATE SEQUENCE job_completion_event_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;


/* Constraints */

ALTER TABLE GPPORTAL.GP_USER_PROP
 ADD FOREIGN KEY (GP_USER_ID)
 REFERENCES GPPORTAL.GP_USER (GP_USER_ID);


CREATE SEQUENCE lsid_suite_identifier_seq
  START WITH 100
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;


CREATE SEQUENCE lsid_identifier_seq
  START WITH 100
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;




/* Data */


INSERT INTO GPPORTAL.JOB_STATUS VALUES(1,'Pending');
INSERT INTO GPPORTAL.JOB_STATUS VALUES(2,'Processing');
INSERT INTO GPPORTAL.JOB_STATUS VALUES(3,'Finished');
INSERT INTO GPPORTAL.JOB_STATUS VALUES(4,'Error');

INSERT INTO GPPORTAL.TASK_ACCESS VALUES(1,'public','public access');
INSERT INTO GPPORTAL.TASK_ACCESS VALUES(2,'private','access only for the owner');

INSERT INTO GPPORTAL.PROPS (KEY, VALUE) VALUES ('schemaVersion', '3.0.0');

COMMIT;














