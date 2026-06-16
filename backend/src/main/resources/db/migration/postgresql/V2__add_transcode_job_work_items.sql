alter table transcode_jobs add column parent_job_id bigint;
alter table transcode_jobs add column work_item varchar(48);
alter table transcode_jobs add column worker_name varchar(80);

update transcode_jobs set work_item = 'batch' where work_item is null;
alter table transcode_jobs alter column work_item set not null;

create index idx_transcode_jobs_parent on transcode_jobs (parent_job_id, requested_at, id);
create index idx_transcode_jobs_work_item on transcode_jobs (work_item);
