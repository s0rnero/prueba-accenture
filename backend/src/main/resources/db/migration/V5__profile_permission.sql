INSERT INTO permissions (code) VALUES ('UPDATE_OWN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE p.code = 'UPDATE_OWN';
