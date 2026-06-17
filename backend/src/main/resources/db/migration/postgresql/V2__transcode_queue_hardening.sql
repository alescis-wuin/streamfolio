alter table catalog_videos
    add column if not exists publication_status varchar(24) not null default 'PUBLISHED';

alter table transcode_jobs
    add column if not exists work_item varchar(48) not null default 'batch',
    add column if not exists parent_job_id bigint,
    add column if not exists worker_name varchar(80),
    add column if not exists attempt_count integer not null default 0,
    add column if not exists max_attempts integer not null default 3,
    add column if not exists next_attempt_at timestamp(6) with time zone,
    add column if not exists cancellation_requested boolean not null default false,
    add column if not exists last_heartbeat_at timestamp(6) with time zone;

update transcode_jobs
set next_attempt_at = requested_at
where next_attempt_at is null
  and status in ('PENDING', 'RETRYING');

alter table transcode_jobs drop constraint if exists ck_transcode_jobs_status;

alter table catalog_videos drop constraint if exists ck_catalog_videos_publication_status;

alter table transcode_jobs
    add constraint ck_transcode_jobs_status
    check (status in ('PENDING', 'RUNNING', 'RETRYING', 'CANCELLING', 'CANCELLED', 'DONE', 'FAILED'));

alter table catalog_videos
    add constraint ck_catalog_videos_publication_status
    check (publication_status in ('DRAFT', 'PUBLISHED'));

create index if not exists idx_transcode_jobs_runnable
    on transcode_jobs (status, next_attempt_at, requested_at, id);

create index if not exists idx_transcode_jobs_parent
    on transcode_jobs (parent_job_id, requested_at, id);

create index if not exists idx_catalog_videos_publication_status
    on catalog_videos (publication_status);
