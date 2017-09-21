create table JOB_COMMENT (
    id integer not null auto_increment,
    gp_job_no integer not null references analysis_job (job_no) on delete cascade,
    -- parent id references id of the job_comment table
    parent_id integer,
    posted_date timestamp not null,
    user_id varchar(255) not null,
    comment_text varchar(1023) not null,
    primary key (id));

create table TAG (
    tag_id integer not null auto_increment,
    tag varchar(255) not null,
    date_added timestamp not null,
    user_id varchar(255) not null,
    public_tag bit default 0 not null,
    primary key (tag_id));
create index idx_tag on TAG(tag);

create table JOB_TAG (
    id integer not null auto_increment,
    gp_job_no integer not null references analysis_job (job_no) on delete cascade,
    date_tagged timestamp not null,
    user_id varchar(255) not null,
    tag_id integer not null references tag (tag_id),
    primary key (id));

