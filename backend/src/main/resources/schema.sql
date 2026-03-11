-- PostgreSQL schema for PDF AI Review app
-- Images are stored as PostgreSQL large objects (OID references)

create table if not exists documents (
  id bigserial primary key,
  file_name text not null,
  original_file_name text,
  book_title text,
  author text,
  book_size text,
  category text,
  pdf_path text not null,
  summary text not null,
  created_at timestamptz not null,
  amazon_url text
);

create table if not exists reviews (
  id bigserial primary key,
  document_id bigint not null unique references documents(id) on delete cascade,
  star_rating integer not null,
  tone text,
  review_title text,
  review_body text not null,
  notes_for_ai text,
  posted boolean not null default false,
  posted_at timestamptz,
  reminder_sent_at timestamptz,
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists document_images (
  id bigserial primary key,
  document_id bigint not null references documents(id) on delete cascade,
  large_object_oid oid not null,
  content_type varchar(100) not null default 'image/jpeg',
  original_file_name text,
  created_at timestamptz not null
);

create index if not exists idx_document_images_document_id on document_images(document_id);
