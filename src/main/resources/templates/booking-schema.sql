CREATE TABLE bookings (
    booking_number VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100),
    date DATE,
    status ENUM('CONFIRMED', 'CANCELLED', 'CHANGED'),
    from_city VARCHAR(50),
    to_city VARCHAR(50),
    booking_class ENUM('ECONOMY', 'BUSINESS', 'FIRST')
);