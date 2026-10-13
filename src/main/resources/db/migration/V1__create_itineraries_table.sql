CREATE TABLE itineraries (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(150) NOT NULL,
    departure_airport_id VARCHAR(50) NOT NULL,
    arrival_airport_id VARCHAR(50) NOT NULL,
    travel_date DATE NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_itineraries_user_name ON itineraries (user_name);
CREATE INDEX idx_itineraries_travel_date ON itineraries (travel_date);
