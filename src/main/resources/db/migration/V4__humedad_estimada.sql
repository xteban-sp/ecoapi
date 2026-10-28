-- SC-C: humedad estimada del material pesado, en porcentaje (0 a 100).
-- Es opcional: los pesajes anteriores al cambio quedan sin valor.
ALTER TABLE pesaje ADD COLUMN humedad_estimada DECIMAL(5,2) CHECK (humedad_estimada BETWEEN 0 AND 100);
