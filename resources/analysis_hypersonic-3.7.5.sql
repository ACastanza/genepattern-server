create table PIN_MODULE (
	id integer generated by default as identity (start with 1),
    username varchar(511),
    lsid varchar(511),
    pin_position double,
    primary key (id)
);
create index idx_pin_module on PIN_MODULE (id);

-- update schema version
update props set value='3.7.5' where key='schemaVersion';

