ALTER TABLE user_roles DROP CONSTRAINT user_roles_pkey;
ALTER TABLE user_roles ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE user_roles ADD CONSTRAINT user_roles_pkey PRIMARY KEY (id);
ALTER TABLE user_roles ADD CONSTRAINT user_roles_user_role_uidx UNIQUE (user_id, role_id);

ALTER TABLE role_permissions DROP CONSTRAINT role_permissions_pkey;
ALTER TABLE role_permissions ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE role_permissions ADD CONSTRAINT role_permissions_pkey PRIMARY KEY (id);
ALTER TABLE role_permissions ADD CONSTRAINT role_permissions_role_perm_uidx UNIQUE (role_id, permission_id);

ALTER TABLE user_franchises DROP CONSTRAINT user_franchises_pkey;
ALTER TABLE user_franchises ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE user_franchises ADD CONSTRAINT user_franchises_pkey PRIMARY KEY (id);
ALTER TABLE user_franchises ADD CONSTRAINT user_franchises_user_franchise_uidx UNIQUE (user_id, franchise_id);
