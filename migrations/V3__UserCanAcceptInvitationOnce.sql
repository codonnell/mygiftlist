ALTER TABLE invitation_acceptance ADD UNIQUE (invitation_id, accepted_by_id);
