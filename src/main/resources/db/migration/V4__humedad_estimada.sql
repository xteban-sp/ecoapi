-- SC-C: humedad estimada del material pesado, en porcentaje (0 a 100).
-- Es opcional: los pesajes anteriores al cambio quedan sin valor.
ALTER TABLE pesaje ADD COLUMN humedad_estimada DECIMAL(5,2) CHECK (humedad_estimada BETWEEN 0 AND 100);

-- Datos semilla actualizados: humedad tipica por material (papel y carton absorben mas).
UPDATE pesaje SET humedad_estimada = CASE material
    WHEN 'CARTON'   THEN 12.50
    WHEN 'PAPEL'    THEN 10.00
    WHEN 'PLASTICO' THEN 2.00
    WHEN 'VIDRIO'   THEN 0.50
    WHEN 'METAL'    THEN 1.00
END;
