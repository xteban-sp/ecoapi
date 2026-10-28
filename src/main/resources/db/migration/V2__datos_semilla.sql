-- Datos semilla del Anexo 10. La base es nueva en cada arranque, asi que los id
-- se generan en el orden de insercion (1, 2, 3...) y se usan subconsultas por nombre
-- donde conviene dejar claro a que fila se refiere.

-- ---------------------------------------------------------------------------
-- Privilegios y roles (matriz de la seccion 3.3)
-- ---------------------------------------------------------------------------
INSERT INTO privilegio (nombre) VALUES
    ('PUNTO_LEER'),
    ('PESAJE_LEER'),
    ('PESAJE_CREAR'),
    ('PESAJE_EDITAR'),
    -- Columna "Todos" de PESAJE_EDITAR: corregir pesajes de otros operarios y de otros dias
    ('PESAJE_EDITAR_TODOS'),
    ('PESAJE_ANULAR'),
    ('RUTA_CERRAR'),
    ('PUNTO_GESTIONAR');

INSERT INTO rol (nombre) VALUES ('OPERARIO'), ('SUPERVISOR'), ('ADMINISTRADOR');

INSERT INTO rol_privilegio (rol_id, privilegio_id)
SELECT r.id, p.id FROM rol r, privilegio p
WHERE r.nombre = 'OPERARIO'
  AND p.nombre IN ('PUNTO_LEER', 'PESAJE_LEER', 'PESAJE_CREAR', 'PESAJE_EDITAR');

INSERT INTO rol_privilegio (rol_id, privilegio_id)
SELECT r.id, p.id FROM rol r, privilegio p
WHERE r.nombre = 'SUPERVISOR'
  AND p.nombre IN ('PUNTO_LEER', 'PESAJE_LEER', 'PESAJE_CREAR', 'PESAJE_EDITAR',
                   'PESAJE_EDITAR_TODOS', 'PESAJE_ANULAR', 'RUTA_CERRAR');

INSERT INTO rol_privilegio (rol_id, privilegio_id)
SELECT r.id, p.id FROM rol r, privilegio p
WHERE r.nombre = 'ADMINISTRADOR'
  AND p.nombre IN ('PUNTO_LEER', 'PESAJE_LEER', 'PUNTO_GESTIONAR');

-- ---------------------------------------------------------------------------
-- Usuarios. Contrasena de todos: Eco2026* (hash BCrypt, coste 10)
-- operario2 existe para demostrar RN-02 (pesaje de otro operario).
-- ---------------------------------------------------------------------------
INSERT INTO usuario (username, password, nombre_completo, rol_id) VALUES
    ('operario1',   '$2a$10$FI2d0OMfSnnfpThoLhdsA.nDy2.QwigACFDgMNWU66T5HnR0ORsR6', 'Luis Quispe Mamani',    (SELECT id FROM rol WHERE nombre = 'OPERARIO')),
    ('supervisor1', '$2a$10$FI2d0OMfSnnfpThoLhdsA.nDy2.QwigACFDgMNWU66T5HnR0ORsR6', 'Rosa Huaman Torres',    (SELECT id FROM rol WHERE nombre = 'SUPERVISOR')),
    ('admin',       '$2a$10$FI2d0OMfSnnfpThoLhdsA.nDy2.QwigACFDgMNWU66T5HnR0ORsR6', 'Carlos Ramirez Vega',   (SELECT id FROM rol WHERE nombre = 'ADMINISTRADOR')),
    ('operario2',   '$2a$10$FI2d0OMfSnnfpThoLhdsA.nDy2.QwigACFDgMNWU66T5HnR0ORsR6', 'Jorge Condori Apaza',   (SELECT id FROM rol WHERE nombre = 'OPERARIO'));

-- ---------------------------------------------------------------------------
-- 12 puntos de acopio en 3 zonas
-- ---------------------------------------------------------------------------
INSERT INTO punto_acopio (razon_social, direccion, zona, material_habitual) VALUES
    ('Textiles Andinos SAC',          'Av. Los Incas 1520',         'NORTE',  'CARTON'),    -- 1
    ('Distribuidora El Sol EIRL',     'Jr. Huascar 340',            'NORTE',  'PLASTICO'),  -- 2
    ('Hospital Regional del Norte',   'Av. Universitaria 2001',     'NORTE',  'PAPEL'),     -- 3
    ('Supermercados Ayni SA',         'Av. Tupac Amaru 815',        'NORTE',  'CARTON'),    -- 4
    ('Embotelladora Illimani SAC',    'Calle Industrial 77',        'CENTRO', 'VIDRIO'),    -- 5
    ('Metalmecanica Collao SRL',      'Av. Industrial 1290',        'CENTRO', 'METAL'),     -- 6
    ('Libreria Universitaria SAC',    'Jr. Lima 455',               'CENTRO', 'PAPEL'),     -- 7
    ('Restaurante La Candelaria',     'Jr. Deustua 210',            'CENTRO', 'VIDRIO'),    -- 8
    ('Clinica San Martin SAC',        'Av. El Sol 980',             'SUR',    'PLASTICO'),  -- 9
    ('Imprenta Grafica Titicaca',     'Jr. Moquegua 128',           'SUR',    'PAPEL'),     -- 10
    ('Ferreteria El Constructor',     'Av. Circunvalacion 3300',    'SUR',    'METAL'),     -- 11
    ('Centro Comercial Plaza Sur',    'Av. Floral 1750',            'SUR',    'CARTON');    -- 12

-- ---------------------------------------------------------------------------
-- 2 rutas: la de ayer (CERRADA, demuestra RN-01) y la de hoy (ABIERTA)
-- ---------------------------------------------------------------------------
INSERT INTO ruta (fecha, operario_id, supervisor_id, estado, cerrada_en) VALUES
    (DATEADD(DAY, -1, CURRENT_DATE),
     (SELECT id FROM usuario WHERE username = 'operario1'),
     (SELECT id FROM usuario WHERE username = 'supervisor1'),
     'CERRADA',
     DATEADD(HOUR, 18, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP))),          -- ruta 1
    (CURRENT_DATE,
     (SELECT id FROM usuario WHERE username = 'operario1'),
     (SELECT id FROM usuario WHERE username = 'supervisor1'),
     'ABIERTA', NULL);                                                                -- ruta 2

-- Ruta de ayer: puntos 7 a 12
INSERT INTO ruta_punto (ruta_id, punto_id, secuencia) VALUES
    (1, 7, 1), (1, 8, 2), (1, 9, 3), (1, 10, 4), (1, 11, 5), (1, 12, 6);

-- Ruta de hoy: puntos 1 a 6 (4, 5 y 6 quedan sin pesar: demuestra RN-04)
INSERT INTO ruta_punto (ruta_id, punto_id, secuencia) VALUES
    (2, 1, 1), (2, 2, 2), (2, 3, 3), (2, 4, 4), (2, 5, 5), (2, 6, 6);

-- ---------------------------------------------------------------------------
-- 15 pesajes
-- ---------------------------------------------------------------------------
-- Ayer (ruta 1, cerrada): 9 pesajes, uno anulado
INSERT INTO pesaje (ruta_id, punto_id, fecha_hora, material, peso_bruto, tara, peso_neto, observacion, operario_id, anulado, motivo_anulacion) VALUES
    (1, 7,  DATEADD(MINUTE, 480, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'PAPEL',    85.50, 5.50, 80.00, '',                          1, TRUE,  'Balanza sin calibrar'),
    (1, 7,  DATEADD(MINUTE, 490, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'PAPEL',    84.00, 5.50, 78.50, 'Repesado',                  1, FALSE, NULL),
    (1, 8,  DATEADD(MINUTE, 530, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'VIDRIO',  120.00, 8.00, 112.00, '',                         1, FALSE, NULL),
    (1, 8,  DATEADD(MINUTE, 535, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'CARTON',   30.00, 2.00, 28.00, 'Cajas sueltas',            1, FALSE, NULL),
    (1, 9,  DATEADD(MINUTE, 580, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'PLASTICO', 45.20, 3.20, 42.00, '',                          1, FALSE, NULL),
    (1, 9,  DATEADD(MINUTE, 585, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'PLASTICO', 22.00, 3.20, 18.80, 'Segundo contenedor',        1, FALSE, NULL),
    (1, 10, DATEADD(MINUTE, 640, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'PAPEL',    66.00, 4.00, 62.00, '',                          1, FALSE, NULL),
    (1, 11, DATEADD(MINUTE, 700, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'METAL',   210.00, 15.00, 195.00, 'Chatarra pesada',         1, FALSE, NULL),
    (1, 12, DATEADD(MINUTE, 760, CAST(DATEADD(DAY, -1, CURRENT_DATE) AS TIMESTAMP)), 'CARTON',   95.00, 6.00, 89.00, '',                          1, FALSE, NULL);

-- Hoy (ruta 2, abierta): 6 pesajes en los puntos 1 a 3; uno anulado y uno de operario2 (RN-02)
INSERT INTO pesaje (ruta_id, punto_id, fecha_hora, material, peso_bruto, tara, peso_neto, observacion, operario_id, anulado, motivo_anulacion) VALUES
    (2, 1, DATEADD(MINUTE, 420, CAST(CURRENT_DATE AS TIMESTAMP)), 'CARTON',   150.00, 10.00, 140.00, '',                     1, FALSE, NULL),
    (2, 1, DATEADD(MINUTE, 425, CAST(CURRENT_DATE AS TIMESTAMP)), 'PLASTICO',  18.00,  2.00,  16.00, 'Film de embalaje',     1, FALSE, NULL),
    (2, 2, DATEADD(MINUTE, 450, CAST(CURRENT_DATE AS TIMESTAMP)), 'PLASTICO',  60.00,  4.00,  56.00, '',                     1, TRUE,  'Material equivocado'),
    (2, 2, DATEADD(MINUTE, 455, CAST(CURRENT_DATE AS TIMESTAMP)), 'PLASTICO',  58.00,  4.00,  54.00, '',                     1, FALSE, NULL),
    (2, 2, DATEADD(MINUTE, 460, CAST(CURRENT_DATE AS TIMESTAMP)), 'CARTON',    12.50,  1.50,  11.00, '',                     1, FALSE, NULL),
    (2, 3, DATEADD(MINUTE, 500, CAST(CURRENT_DATE AS TIMESTAMP)), 'PAPEL',     40.00,  3.00,  37.00, 'Registrado por apoyo', 4, FALSE, NULL);
