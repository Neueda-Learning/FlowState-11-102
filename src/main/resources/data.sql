INSERT IGNORE INTO account (
	account_number,
	account_holder_name,
	email,
	phone_number,
	balance,
	currency,
	status
)
VALUES
	('ACC1003', 'Carol Dsouza',  'carol@example.com',   '9876501234',  8200.00, 'INR', 'ACTIVE'),
	('ACC1004', 'David Kumar',   'david@example.com',   '9123456780', 12000.00, 'INR', 'ACTIVE'),
	('ACC1005', 'Eva Thomas',    'eva@example.com',     '9345678901',  3100.00, 'INR', 'INACTIVE'),
	('ACC1006', 'Farhan Ali',    'farhan@example.com',  '9456789012',  1500.00, 'INR', 'BLOCKED');

