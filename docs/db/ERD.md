# ENTITY RELATIONSHIP DIAGRAM — Opportunity Board

> Sinh từ `docs/db/schema.sql` (PostgreSQL). Mermaid ER diagram.

```mermaid
erDiagram
    users {
        uuid user_id PK
        string email UK
        string password_hash
        string full_name
        user_role role
        user_status status
        auth_provider auth_provider
        timestamp email_verified_at
        timestamp last_login_at
        boolean mfa_enabled
        smallint failed_login_count
        timestamp locked_until
        int password_version
    }
    student_profiles {
        uuid profile_id PK
        uuid user_id FK
        string major
        string university
        smallint university_year
        string cv_url
        jsonb skills
        text bio
    }
    organizations {
        uuid org_id PK
        uuid owner_user_id FK
        string org_name
        string logo_url
        string website
        text description
        org_verified verified_status
        timestamp verified_at
        uuid verified_by FK
    }
    org_members {
        uuid org_member_id PK
        uuid org_id FK
        uuid user_id FK
        string member_role
    }
    categories {
        uuid category_id PK
        string code UK
        string category_name
        int display_order
        boolean is_system
    }
    domains {
        uuid domain_id PK
        string domain_name UK
    }
    opportunities {
        uuid opp_id PK
        uuid org_id FK
        uuid created_by FK
        uuid category_id FK
        string title
        string slug UK
        text description
        work_type work_type
        location_type location
        apply_mode apply_mode
        string external_link
        timestamp deadline
        opp_status status
        text rejection_reason
        uuid moderated_by FK
        boolean is_featured
        uuid featured_by FK
        timestamp featured_until
        int view_count
        int bookmark_count
        int application_count
    }
    opportunity_domains {
        uuid opp_id FK
        uuid domain_id FK
    }
    applications {
        uuid app_id PK
        uuid opp_id FK
        uuid student_id FK
        boolean is_external
        string cv_file
        text cover_letter
        app_status status
        text provider_note
        text rejection_reason
        uuid updated_by FK
    }
    application_status_history {
        uuid id PK
        uuid app_id FK
        app_status from_status
        app_status to_status
        uuid changed_by FK
    }
    saved_opportunities {
        uuid id PK
        uuid student_id FK
        uuid opp_id FK
        smallint notify_before_hours
    }
    notifications {
        uuid notification_id PK
        uuid user_id FK
        notif_type type
        notif_channel channel
        string title
        uuid ref_id
        boolean is_read
    }
    notification_preferences {
        uuid user_id FK
        notif_type type
        notif_channel channel
        boolean enabled
        notif_frequency frequency
        jsonb categories
        jsonb domains
    }
    device_tokens {
        uuid id PK
        uuid user_id FK
        string token
        string platform
    }
    moderation_logs {
        uuid id PK
        uuid opp_id FK
        uuid admin_id FK
        moderation_action action
        text reason
    }
    audit_logs {
        uuid id PK
        uuid actor_id FK
        string action
        string entity
        uuid entity_id
    }

    users ||--o| student_profiles : "1-1"
    users ||--o| organizations : "owns (owner_user_id)"
    users ||--o{ org_members : "member of"
    organizations ||--o{ org_members : "has members"
    organizations ||--o{ opportunities : "posts"
    categories ||--o{ opportunities : "classifies"
    opportunities ||--o{ opportunity_domains : "tagged"
    domains ||--o{ opportunity_domains : "tags"
    opportunities ||--o{ applications : "receives"
    users ||--o{ applications : "student applies"
    applications ||--o{ application_status_history : "tracks"
    users ||--o{ saved_opportunities : "saves"
    opportunities ||--o{ saved_opportunities : "saved by"
    users ||--o{ notifications : "receives"
    users ||--o{ notification_preferences : "prefers"
    users ||--o{ device_tokens : "registers"
    users ||--o{ moderation_logs : "admin acts"
    opportunities ||--o{ moderation_logs : "moderated"
    users ||--o{ audit_logs : "actor"
```

## Chú thích ràng buộc quan trọng

- `applications UNIQUE(opp_id, student_id)` — chặn nộp trùng (S5).
- `opportunities CHECK`: nếu `apply_mode=EXTERNAL` thì `external_link` bắt buộc (Mục 4).
- `opportunities CHECK`: `deadline > created_at`.
- `saved_opportunities.notify_before_hours` trong [24,48].
- `opportunity_display_status` VIEW: tách status lưu (DRAFT/PENDING/APPROVED/...) khỏi status hiển thị (OPEN/CLOSING_SOON/EXPIRED/HIDDEN).
- Quan hệ sở hữu: `applications.opp_id → opportunities.org_id → organizations(owner_user_id / org_members)` — dùng enforce RBAC chống IDOR.
