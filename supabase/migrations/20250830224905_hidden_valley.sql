-- Client Management Database Setup Script
-- This script creates the complete database schema for the Java client management application

-- Create database
CREATE DATABASE IF NOT EXISTS client_management;
USE client_management;

-- Drop existing tables if they exist (in correct order due to foreign keys)
DROP TABLE IF EXISTS versement;
DROP TABLE IF EXISTS clients;

-- Create clients table
CREATE TABLE clients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100),
    activite VARCHAR(200) NOT NULL,
    annee VARCHAR(10),
    agent_responsable VARCHAR(100),
    forme_juridique VARCHAR(100),
    regime_fiscal VARCHAR(100),
    regime_cnas VARCHAR(100),
    mode_paiement VARCHAR(100),
    indicateur VARCHAR(100),
    recette_impots VARCHAR(100),
    observation TEXT,
    source INT,
    honoraires_mois VARCHAR(50),
    montant DECIMAL(15,2),
    phone VARCHAR(20),
    email VARCHAR(100),
    company VARCHAR(200),
    address TEXT,
    type VARCHAR(50),
    premier_versement VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Add indexes for better performance
    INDEX idx_nom (nom),
    INDEX idx_activite (activite),
    INDEX idx_company (company),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
);

-- Create versement table (note: your DAO uses 'versement' not 'versments')
CREATE TABLE versement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    montant DECIMAL(15,2) NOT NULL,
    type VARCHAR(50) NOT NULL,
    date_paiement DATE NOT NULL,
    annee_concernee VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
    
    -- Add indexes for better performance
    INDEX idx_client_id (client_id),
    INDEX idx_date_paiement (date_paiement),
    INDEX idx_annee_concernee (annee_concernee),
    INDEX idx_type (type)
);

-- Insert sample data for testing
INSERT INTO clients (
    nom, prenom, activite, annee, forme_juridique, regime_fiscal, 
    regime_cnas, recette_impots, observation, source, honoraires_mois, 
    montant, phone, email, company, address, type, premier_versement
) VALUES 
(
    'Dupont', 'Jean', 'Comptabilité', '2024', 'SARL', 'Réel Normal', 
    'Général', 'Centre des Impôts', 'Client régulier', 1, '5000', 
    60000.00, '0123456789', 'jean.dupont@email.com', 'Comptabilité Dupont', 
    '123 Rue de la Paix, Alger', 'Premium', '2024-01-15'
),
(
    'Martin', 'Sophie', 'Conseil Juridique', '2024', 'SNC', 'Simplifié', 
    'Général', 'Centre des Impôts', 'Nouveau client', 2, '3000', 
    36000.00, '0987654321', 'sophie.martin@email.com', 'Cabinet Martin', 
    '456 Avenue des Affaires, Oran', 'Standard', '2024-02-01'
),
(
    'Benali', 'Ahmed', 'Commerce', '2024', 'EURL', 'Réel Normal', 
    'Général', 'Centre des Impôts', 'Client fidèle depuis 3 ans', 3, '4000', 
    48000.00, '0555123456', 'ahmed.benali@email.com', 'Commerce Benali', 
    '789 Boulevard Commercial, Constantine', 'Premium', '2024-01-10'
);

-- Insert sample versements
INSERT INTO versement (client_id, montant, type, date_paiement, annee_concernee) VALUES 
(1, 5000.00, 'Honoraires', '2024-01-15', '2024'),
(1, 5000.00, 'Honoraires', '2024-02-15', '2024'),
(2, 3000.00, 'Honoraires', '2024-02-01', '2024'),
(3, 4000.00, 'Honoraires', '2024-01-10', '2024'),
(3, 4000.00, 'Honoraires', '2024-02-10', '2024');

-- Update clients montant to reflect versements already made
UPDATE clients c 
SET montant = (
    SELECT COALESCE(
        (SELECT SUM(COALESCE(honoraires_mois, 0) * 12) FROM clients WHERE id = c.id), 
        0
    ) - COALESCE(
        (SELECT SUM(montant) FROM versement WHERE client_id = c.id), 
        0
    )
) 
WHERE id IN (1, 2, 3);

-- Create a view for easy reporting (optional)
CREATE OR REPLACE VIEW client_versement_summary AS
SELECT 
    c.id,
    c.nom,
    c.prenom,
    c.company,
    c.activite,
    c.montant as montant_restant,
    COALESCE(v.total_versements, 0) as total_versements,
    (c.montant + COALESCE(v.total_versements, 0)) as montant_initial,
    CASE 
        WHEN c.montant = 0 THEN 'Entièrement payé'
        WHEN c.montant < 1000 THEN 'Presque payé'
        ELSE 'En cours'
    END as statut_paiement
FROM clients c
LEFT JOIN (
    SELECT 
        client_id, 
        SUM(montant) as total_versements,
        COUNT(*) as nombre_versements
    FROM versement 
    GROUP BY client_id
) v ON c.id = v.client_id;

-- Show the created tables structure
SHOW TABLES;

-- Display sample data
SELECT 'CLIENTS TABLE:' as info;
SELECT id, nom, prenom, activite, montant, phone, company FROM clients;

SELECT 'VERSEMENTS TABLE:' as info;
SELECT id, client_id, montant, type, date_paiement, annee_concernee FROM versement;

SELECT 'CLIENT-VERSEMENT SUMMARY:' as info;
SELECT * FROM client_versement_summary;

COMMIT;