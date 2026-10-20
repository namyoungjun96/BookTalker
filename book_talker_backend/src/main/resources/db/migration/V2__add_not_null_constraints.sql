ALTER TABLE public.review ALTER COLUMN content SET NOT NULL;

ALTER TABLE public.review ALTER COLUMN headline SET NOT NULL;

ALTER TABLE public.review ALTER COLUMN visibility SET NOT NULL;
ALTER TABLE public.review ALTER COLUMN visibility DROP DEFAULT;

ALTER TABLE public.team_member ALTER COLUMN team_id SET NOT NULL;
