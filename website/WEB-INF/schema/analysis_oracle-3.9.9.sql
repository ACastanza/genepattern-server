-- 
-- additional built-in patch_info entries
-- 

insert into patch_info (id, lsid) select 
        patch_info_SEQ.nextVal, 'urn:lsid:broadinstitute.org:plugin:Ant_1.8:1' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broadinstitute.org:plugin:Ant_1.8:1' );
commit;

insert into patch_info (id, lsid) select 
        patch_info_SEQ.nextVal, 'urn:lsid:broadinstitute.org:plugin:Ant_1.8' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broadinstitute.org:plugin:Ant_1.8' );
commit;

insert into patch_info (id, lsid) select 
        patch_info_SEQ.nextVal, 'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:GenePattern_3_4_2:2' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:GenePattern_3_4_2:2' );
commit;

insert into patch_info (id, lsid) select 
        patch_info_SEQ.nextVal, 'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_1:0.1' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_1:0.1' );
commit;

insert into patch_info (id, lsid) select 
        patch_info_SEQ.nextVal, 'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_3:1' as lsid from dual
    where not exists ( select lsid from patch_info where lsid = 
        'urn:lsid:broadinstitute.org:plugin:GenePattern_3_9_3:1' );
commit;

-- 
-- update GP_USER_PROP 'ModuleRepositoryURL' value 
-- 
update gp_user_prop
    set value = replace (
        value, 'www.broadinstitute.org', 'software.broadinstitute.org')
where key = 'ModuleRepositoryURL';
commit;
