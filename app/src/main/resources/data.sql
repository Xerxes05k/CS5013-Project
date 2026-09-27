-- Seed data for local development only. Real residents are loaded from the hostel
-- list before the pilot; this file exists so `./mvnw spring-boot:run` gives you a
-- usable screen on a fresh database.
--
-- Demo login: ce24b128@smail.iitm.ac.in / passcode "cycle1234" (dev only — the
-- pilot generates per-resident passcodes and this row is not loaded in production).

INSERT INTO residents (roll_number, name, hostel, smail_address, passcode_hash)
SELECT 'CE24B128', 'Yashas Katyal', 'Jamuna', 'ce24b128@smail.iitm.ac.in',
       '$2a$10$ciF1Xe0DquPWmCV5gRYGUeZoF4eVwB.B/72CTKUg5z.ZNgde.YlZm'
WHERE NOT EXISTS (SELECT 1 FROM residents WHERE roll_number = 'CE24B128');

INSERT INTO cycles (label, state, version)
SELECT 'JAM-01', 'AVAILABLE', 0 WHERE NOT EXISTS (SELECT 1 FROM cycles WHERE label = 'JAM-01');
INSERT INTO cycles (label, state, version)
SELECT 'JAM-02', 'AVAILABLE', 0 WHERE NOT EXISTS (SELECT 1 FROM cycles WHERE label = 'JAM-02');
INSERT INTO cycles (label, state, version)
SELECT 'JAM-03', 'ISSUED', 0 WHERE NOT EXISTS (SELECT 1 FROM cycles WHERE label = 'JAM-03');
INSERT INTO cycles (label, state, version)
SELECT 'JAM-04', 'AVAILABLE', 0 WHERE NOT EXISTS (SELECT 1 FROM cycles WHERE label = 'JAM-04');
INSERT INTO cycles (label, state, version)
SELECT 'JAM-05', 'UNDER_REPAIR', 0 WHERE NOT EXISTS (SELECT 1 FROM cycles WHERE label = 'JAM-05');
