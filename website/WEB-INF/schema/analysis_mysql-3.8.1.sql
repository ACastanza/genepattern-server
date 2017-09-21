create table JOB_INPUT_VALUE (
    gp_job_no integer not null,
    idx integer default 1 not null,
    pname varchar(511) not null,
    pvalue varchar(1023) default '',
    group_id varchar(511) default '',
    group_name varchar(511) default '',
    primary key (gp_job_no, idx, pname),
    constraint jiv_gpjn_fk foreign key (GP_JOB_NO) references ANALYSIS_JOB(JOB_NO) on delete cascade
);


--
-- small change to make it easier to remove eula records
-- 
alter table EULA_REMOTE_QUEUE
    add constraint ERQ_FK
    FOREIGN KEY (eula_record_id) references EULA_RECORD(id)
    on delete cascade;
commit;
