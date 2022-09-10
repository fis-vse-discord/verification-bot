drop table if exists pending_verifications;

create table pending_verifications(
    username varchar(8) not null unique primary key,
    code varchar(32) not null unique
);
