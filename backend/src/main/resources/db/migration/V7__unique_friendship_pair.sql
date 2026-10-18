-- One request or friendship per pair of users, whoever sent it: two people
-- sending each other a request at the same moment must not end up with two rows.
CREATE UNIQUE INDEX IF NOT EXISTS uq_friendship_pair
    ON friendship (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
