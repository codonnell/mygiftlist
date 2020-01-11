CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;

CREATE TABLE "user" (
  id uuid PRIMARY KEY DEFAULT public.gen_random_uuid(),
  email text NOT NULL UNIQUE,
  auth0_id text NOT NULL UNIQUE,
  given_name text,
  family_name text,
  allow_name_access boolean DEFAULT false NOT NULL,
  created_at timestamp with time zone DEFAULT now() NOT NULL
);


CREATE TABLE gift_list (
    id uuid PRIMARY KEY DEFAULT public.gen_random_uuid(),
    name text NOT NULL,
    created_by_id uuid NOT NULL REFERENCES "user" (id),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


CREATE TABLE gift (
  id uuid PRIMARY KEY DEFAULT public.gen_random_uuid(),
  name text NOT NULL,
  description text,
  url text,
  requested_by_id uuid NOT NULL REFERENCES "user" (id),
  requested_at timestamp with time zone NOT NULL,
  claimed_by_id uuid REFERENCES "user" (id),
  claimed_at timestamp with time zone,
  gift_list_id uuid NOT NULL REFERENCES gift_list (id)
);


CREATE TABLE invitation (
    id uuid PRIMARY KEY DEFAULT public.gen_random_uuid(),
    token text UNIQUE NOT NULL,
    gift_list_id uuid NOT NULL REFERENCES gift_list (id),
    created_by_id uuid NOT NULL REFERENCES "user" (id),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    expires_at timestamp with time zone DEFAULT (now() + '7 days'::interval) NOT NULL
);


CREATE TABLE invitation_acceptance (
    id uuid PRIMARY KEY DEFAULT public.gen_random_uuid(),
    invitation_id uuid NOT NULL REFERENCES invitation (id),
    accepted_by_id uuid NOT NULL REFERENCES "user" (id),
    accepted_at timestamp with time zone DEFAULT now() NOT NULL
);


CREATE TABLE revocation (
    id uuid PRIMARY KEY DEFAULT public.gen_random_uuid(),
    gift_list_id uuid NOT NULL REFERENCES gift_list (id),
    created_by_id uuid NOT NULL REFERENCES "user" (id),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    revoked_user_id uuid NOT NULL REFERENCES "user" (id)
);


CREATE VIEW gift_list_access AS
 SELECT gl.id AS gift_list_id,
    u.auth0_id
   FROM (public.gift_list gl
     JOIN public."user" u ON ((u.id = gl.created_by_id)))
UNION
 SELECT gl.id AS gift_list_id,
    u.auth0_id
   FROM ((((public."user" u
     JOIN public.invitation_acceptance ia ON ((ia.accepted_by_id = u.id)))
     JOIN public.invitation i ON ((i.id = ia.invitation_id)))
     JOIN public.gift_list gl ON ((gl.id = i.gift_list_id)))
     LEFT JOIN public.revocation r ON (((r.gift_list_id = gl.id) AND (r.revoked_user_id = u.id))))
  WHERE (r.id IS NULL);
