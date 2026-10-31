-- V11 contains a public demo administrator password. Never expose it on the VPS.
-- Preserve a deliberately rotated account: match both the seed id and original hash.
UPDATE users
SET status = 'LOCKED', updated_at = CURRENT_TIMESTAMP
WHERE id = 'a0000000-0000-0000-0000-000000000001'
  AND password = '$argon2id$v=19$m=16384,t=2,p=1$JbG5+9dhUob+DqvUOL1s7g$mBSR4ifrExNP/FD7o2U/Y3DEpiWifAcBmpVCmJpGF9Q';
