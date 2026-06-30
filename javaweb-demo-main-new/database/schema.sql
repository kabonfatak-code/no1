create
database bbs;
use
bbs;

create table users
(
    id              bigint primary key auto_increment,
    username        varchar(30) not null unique,
    password_hash   char(64)    not null,
    phone           varchar(20) not null unique,
    province        varchar(20) null,
    role            varchar(20) not null default 'NEW_USER',
    banned          tinyint(1) not null default 0,
    banned_until    timestamp null,
    history_enabled tinyint(1) not null default 1,
    created_at      timestamp   not null default current_timestamp,
    register_time   timestamp   not null default current_timestamp
)comment "用户表";

create table posts
(
    id             bigint primary key auto_increment,
    title          varchar(120) not null,
    topic          varchar(30)  not null,
    region         varchar(30)  not null,
    content        text         not null,
    author_id      bigint       not null,
    ip_address     varchar(45) null,
    pinned         tinyint(1) not null default 0,
    deleted        tinyint(1) not null default 0,
    like_score     int          not null default 0,
    dislike_score  int          not null default 0,
    favorite_count int          not null default 0,
    comment_count  int          not null default 0,
    created_at     timestamp    not null default current_timestamp,
    updated_at     timestamp    not null default current_timestamp on update current_timestamp,
    index          idx_posts_search(topic, region, created_at),
    index          idx_posts_rank(pinned, like_score, favorite_count),
    constraint fk_posts_author foreign key (author_id) references users (id)
)comment "留言表";

create table comments
(
    id                bigint primary key auto_increment,
    post_id           bigint    not null,
    parent_comment_id bigint null,
    author_id         bigint    not null,
    ip_address        varchar(45) null,
    region            varchar(30) null,
    content           text      not null,
    deleted           tinyint(1) not null default 0,
    like_score        int       not null default 0,
    dislike_score     int       not null default 0,
    created_at        timestamp not null default current_timestamp,
    updated_at        timestamp not null default current_timestamp on update current_timestamp,
    index             idx_comments_post(post_id, created_at),
    index             idx_comments_parent(parent_comment_id),
    constraint fk_comments_post foreign key (post_id) references posts (id),
    constraint fk_comments_parent foreign key (parent_comment_id) references comments (id),
    constraint fk_comments_author foreign key (author_id) references users (id)
)comment "评论表";

create table post_votes
(
    user_id    bigint    not null,
    post_id    bigint    not null,
    value      tinyint   not null,
    weight     int       not null,
    created_at timestamp not null default current_timestamp,
    primary key (user_id, post_id),
    constraint fk_post_votes_user foreign key (user_id) references users (id),
    constraint fk_post_votes_post foreign key (post_id) references posts (id)
)comment "留言举报表";

create table comment_votes
(
    user_id    bigint    not null,
    comment_id bigint    not null,
    value      tinyint   not null,
    weight     int       not null,
    created_at timestamp not null default current_timestamp,
    primary key (user_id, comment_id),
    constraint fk_comment_votes_user foreign key (user_id) references users (id),
    constraint fk_comment_votes_comment foreign key (comment_id) references comments (id)
)comment "评论举报表";

create table favorites
(
    user_id    bigint    not null,
    post_id    bigint    not null,
    created_at timestamp not null default current_timestamp,
    primary key (user_id, post_id),
    constraint fk_favorites_user foreign key (user_id) references users (id),
    constraint fk_favorites_post foreign key (post_id) references posts (id)
)comment "收藏表";

create table browse_history
(
    user_id   bigint    not null,
    post_id   bigint    not null,
    viewed_at timestamp not null default current_timestamp,
    primary key (user_id, post_id),
    constraint fk_history_user foreign key (user_id) references users (id),
    constraint fk_history_post foreign key (post_id) references posts (id)
)comment "历史浏览表";

create table reports
(
    id          bigint primary key auto_increment,
    reporter_id bigint       not null,
    post_id     bigint null,
    comment_id  bigint null,
    reason      varchar(255) not null,
    weight      int          not null,
    handled     tinyint(1) not null default 0,
    created_at  timestamp    not null default current_timestamp,
    index       idx_reports_handled(handled, created_at),
    constraint fk_reports_user foreign key (reporter_id) references users (id),
    constraint fk_reports_post foreign key (post_id) references posts (id),
    constraint fk_reports_comment foreign key (comment_id) references comments (id)
)comment "举报处理表";

create table notifications
(
    id           bigint primary key auto_increment,
    recipient_id bigint       not null,
    actor_id     bigint       not null,
    post_id      bigint null,
    comment_id   bigint null,
    type         varchar(30)  not null,
    message      varchar(255) not null,
    read_flag    tinyint(1) not null default 0,
    created_at   timestamp    not null default current_timestamp,
    index        idx_notifications_user(recipient_id, read_flag, created_at),
    constraint fk_notifications_recipient foreign key (recipient_id) references users (id),
    constraint fk_notifications_actor foreign key (actor_id) references users (id)
)comment "信息表";

insert into users(username, password_hash, phone, role)
select 'admin', sha2('admin123', 256), '10000', 'ADMIN';

create unique index idx_reports_post on reports (post_id, reporter_id);
create unique index idx_reports_comment on reports (comment_id, reporter_id);
