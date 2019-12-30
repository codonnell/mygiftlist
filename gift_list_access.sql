-- TODO: Add this when we add proper database migrations
create view gift_list_access as
  (select gl.id as gift_list_id, u.auth0_id
     from gift_list as gl
            join "user" as u on u.id = gl.created_by_id)
  union
  (select gl.id as gift_list_id, u.auth0_id
     from "user" as u
            join invitation_acceptance as ia on ia.accepted_by_id = u.id
            join invitation as i on i.id = ia.invitation_id
            join gift_list as gl on gl.id = i.gift_list_id
            left join revocation r on r.gift_list_id = gl.id and r.revoked_user_id = u.id
    where r.id is null);
