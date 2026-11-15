-- Staff sign in with their Active Directory account; this links the directory login to the app user.
alter table users add column directory_username varchar(64);
create unique index ux_users_directory_username on users (lower(directory_username));
