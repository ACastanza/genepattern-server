-- add table(s) for System Alert Messages.

CREATE TABLE GPPORTAL.SYSTEM_MESSAGE ( 
    id              NUMBER (10,0) NOT NULL,
    message         VARCHAR2(4000 CHAR) NOT NULL,
    start_time      TIMESTAMP DEFAULT sysdate NOT NULL,
    end_time        TIMESTAMP NULL,
    deleteOnRestart NUMBER (1,0) NOT NULL,
    PRIMARY KEY (id)
    );

CREATE SEQUENCE SYSTEM_MESSAGE_SEQ
  START WITH 1
  MAXVALUE 999999999999999999999999999
  MINVALUE 0
  NOCYCLE
  NOCACHE
  NOORDER;

-- update schema version
UPDATE GPPORTAL.PROPS SET VALUE = '3.1.1' where KEY = 'schemaVersion';

COMMIT;














