-- Meta pelos ciclos: só os gastos dos ciclos marcados entram no valor necessário do objetivo.
-- Padrão do catálogo: Preparação e Imigração (o que precisa estar pago antes de viajar).
ALTER TABLE expense_cycle ADD COLUMN in_goal boolean NOT NULL DEFAULT false;

UPDATE expense_cycle
SET in_goal = true
WHERE lower(name) IN ('preparação', 'preparacao', 'imigração', 'imigracao');
