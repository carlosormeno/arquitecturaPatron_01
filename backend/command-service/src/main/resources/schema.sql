CREATE TABLE IF NOT EXISTS producto (
                                        id TEXT PRIMARY KEY,
                                        nombre TEXT NOT NULL,
                                        precio NUMERIC(10, 2) NOT NULL
    );