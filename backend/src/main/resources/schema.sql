create table if not exists documents (
  id integer primary key autoincrement,
  file_name text not null,
  original_file_name text,
  book_title text,
  author text,
  book_size text,
  category text,
  pdf_path text not null,
  summary text not null,
  created_at text not null,
  amazon_url text
);

create table if not exists reviews (
  id integer primary key autoincrement,
  document_id integer not null unique,
  star_rating integer not null,
  tone text,
  review_title text,
  review_body text not null,
  notes_for_ai text,
  posted integer not null default 0,
  posted_at text,
  reminder_sent_at text,
  created_at text not null,
  updated_at text not null,
  foreign key(document_id) references documents(id)
);

create table if not exists document_images (
  id integer primary key autoincrement,
  document_id integer not null,
  file_path text not null,
  original_file_name text,
  created_at text not null,
  foreign key(document_id) references documents(id)
);

