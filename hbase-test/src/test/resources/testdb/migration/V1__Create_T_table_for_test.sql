CREATE TABLE IF NOT EXISTS T (
    A VARCHAR NOT NULL,
    B VARCHAR NULL,
    CONSTRAINT pk PRIMARY KEY(A)
) DATA_BLOCK_ENCODING='FAST_DIFF',KEEP_DELETED_CELLS=false,TTL=7776000,UPDATE_CACHE_FREQUENCY=3600000;
