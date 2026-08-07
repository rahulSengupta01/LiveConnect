# LiveConnect Database Design

## User

| Column          | Type    | Description |
|------------     |------   |-------------|
| id              | UUID    | Primary Key |
| username        | VARCHAR | Unique username |
| email           | VARCHAR | User email |
| password        | VARCHAR | Encrypted password |
| first_name      | VARCHAR | First Name |
| last_name       | VARCHAR | Last Name |
| profile_picture | VARCHAR | Profile picture URL |
| bio             | TEXT    | User bio |
| phone_number    | VARCHAR | Phone Number |
| status          | ENUM    | ONLINE/OFFLINE/AWAY/BUSY |
| last_seen       | TIMESTAMP | Last active |
| email_verified  | BOOLEAN   | Email verification status |
| created_at      | TIMESTAMP | Created time |
| updated_at      | TIMESTAMP | Updated time |