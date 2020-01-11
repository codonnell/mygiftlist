CREATE INDEX ON gift_list USING HASH (created_by_id);

CREATE INDEX ON gift USING HASH (requested_by_id);
CREATE INDEX ON gift USING HASH (claimed_by_id);
CREATE INDEX ON gift USING HASH (gift_list_id);

CREATE INDEX ON invitation USING HASH (gift_list_id);
CREATE INDEX ON invitation USING HASH (created_by_id);

CREATE INDEX ON invitation_acceptance USING HASH (invitation_id);
CREATE INDEX ON invitation_acceptance USING HASH (accepted_by_id);

CREATE INDEX ON revocation USING HASH (gift_list_id);
CREATE INDEX ON revocation USING HASH (created_by_id);
CREATE INDEX ON revocation USING HASH (revoked_user_id);
