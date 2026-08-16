CREATE TABLE products (
    id NUMBER PRIMARY KEY,
    product_code VARCHAR2(50) UNIQUE NOT NULL,
    name VARCHAR2(150) NOT NULL,
    product_category VARCHAR2(30) NOT NULL,
    product_type VARCHAR2(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE product_versions (
    id NUMBER PRIMARY KEY,
    product_id NUMBER NOT NULL,
    version_number NUMBER NOT NULL,
    status VARCHAR2(20) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_to TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_product_versions_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT uk_product_version UNIQUE (product_id, version_number),
    CONSTRAINT uk_product_version_pair UNIQUE (id, product_id),
    CONSTRAINT ck_product_version_dates CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE TABLE product_attribute_definitions (
    id NUMBER PRIMARY KEY,
    attribute_code VARCHAR2(50) UNIQUE NOT NULL,
    display_name VARCHAR2(100) NOT NULL,
    data_type VARCHAR2(20) NOT NULL,
    unit VARCHAR2(20),
    CONSTRAINT ck_attribute_data_type CHECK (data_type IN ('STRING', 'INTEGER', 'DECIMAL', 'BOOLEAN', 'DATE'))
);

CREATE TABLE product_version_attributes (
    id NUMBER PRIMARY KEY,
    product_version_id NUMBER NOT NULL,
    attribute_id NUMBER NOT NULL,
    attribute_value VARCHAR2(500) NOT NULL,
    CONSTRAINT fk_pva_product_version FOREIGN KEY (product_version_id) REFERENCES product_versions(id),
    CONSTRAINT fk_pva_attribute FOREIGN KEY (attribute_id) REFERENCES product_attribute_definitions(id),
    CONSTRAINT uk_pva_version_attribute UNIQUE (product_version_id, attribute_id)
);

CREATE SEQUENCE products_seq START WITH 10 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE product_versions_seq START WITH 902 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE product_attribute_definitions_seq START WITH 17 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE product_version_attributes_seq START WITH 1806 INCREMENT BY 1 NOCACHE NOCYCLE;
