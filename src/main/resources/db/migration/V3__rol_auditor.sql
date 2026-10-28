-- SC-B: rol AUDITOR con todos los privilegios de lectura y ninguno de escritura.
-- El rol se agrega donde viven los privilegios (rol_privilegio). No hace falta tocar
-- SecurityConfig, el filtro JWT ni los controladores: ninguno pregunta por el nombre del rol.
INSERT INTO rol (nombre) VALUES ('AUDITOR');

INSERT INTO rol_privilegio (rol_id, privilegio_id)
SELECT r.id, p.id FROM rol r, privilegio p
WHERE r.nombre = 'AUDITOR'
  AND p.nombre IN ('PUNTO_LEER', 'PESAJE_LEER');

-- Usuario de prueba del nuevo rol (contrasena Eco2026*, BCrypt)
INSERT INTO usuario (username, password, nombre_completo, rol_id) VALUES
    ('auditor1', '$2a$10$FI2d0OMfSnnfpThoLhdsA.nDy2.QwigACFDgMNWU66T5HnR0ORsR6', 'Ana Mamani Choque',
     (SELECT id FROM rol WHERE nombre = 'AUDITOR'));
