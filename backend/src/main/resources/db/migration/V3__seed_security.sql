INSERT INTO roles (code) VALUES ('ADMIN'), ('USER');

INSERT INTO permissions (code) VALUES ('READ'), ('CREATE'), ('UPDATE'), ('DELETE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'READ'
WHERE r.code = 'USER';
