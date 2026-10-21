"""allergen catalog

Revision ID: 0007
Revises: 0006

* new table allergens (reference data, not a Record): what parents pick for a child and what the
  weekly menu is checked against (POST /estancias/{id}/menu-check)
* seeded here, for new and existing databases alike
"""
import uuid
from datetime import datetime, timezone

from alembic import op
import sqlalchemy as sa

revision = '0007'
down_revision = '0006'
branch_labels = None
depends_on = None

S = 'public'

# name (shown to parents and stored on the child, no commas) -> what contains it, for Jev
ALLERGENS = {
    'Leche y lácteos': 'leche, queso, yogur, crema, mantequilla, requesón, suero de leche, caseína, lactosa, '
                       'leche condensada, cajeta, helado, natilla',
    'Huevo': 'huevo, clara, yema, mayonesa, merengue, rebozado, capeado, pan dulce, hot cakes, pasteles, flan',
    'Cacahuate': 'cacahuate, maní, crema de cacahuate, mazapán, salsa de cacahuate, mole, garapiñados',
    'Nueces y frutos secos': 'nuez, almendra, avellana, nuez de la india, pistache, piñón, macadamia, '
                             'nutella, turrón, mole',
    'Trigo y gluten': 'trigo, harina, pan, tortilla de harina, pasta, sopa de fideo, galletas, cebada, centeno, '
                      'avena, empanizado, capeado, bolillo, pastel',
    'Soya': 'soya, salsa de soya, tofu, leche de soya, lecitina de soya, edamame, proteína de soya',
    'Pescado': 'pescado, atún, salmón, tilapia, sardina, bacalao, filete de pescado, caldo de pescado, '
               'salsa inglesa, surimi',
    'Mariscos': 'camarón, langosta, langostino, cangrejo, jaiba, surimi, coctel de mariscos, caldo de camarón',
    'Moluscos': 'pulpo, calamar, almeja, ostión, mejillón, caracol, abulón',
    'Ajonjolí': 'ajonjolí, sésamo, tahini, aceite de ajonjolí, mole, pan con ajonjolí',
    'Mostaza': 'mostaza, aderezos, salsas preparadas, curry',
    'Apio': 'apio, caldos, sopas, consomé, ensaladas',
    'Sulfitos': 'sulfitos, frutas secas, pasas, embutidos, salchicha, jamón, vinagre, jugos envasados',
    'Maíz': 'maíz, elote, tortilla de maíz, tamal, atole, pozole, palomitas, fécula de maíz, jarabe de maíz, '
            'totopos, sopes, quesadillas',
    'Fresa': 'fresa, mermelada de fresa, yogur de fresa, licuado de fresa, gelatina de fresa',
    'Kiwi': 'kiwi, ensalada de frutas',
    'Durazno': 'durazno, melocotón, chabacano, almíbar, coctel de frutas',
    'Cacao y chocolate': 'chocolate, cacao, chocolate en polvo, galletas de chocolate, mole, pastel de chocolate',
    'Colorantes artificiales': 'colorantes, rojo 40, amarillo 5, gelatina de colores, dulces, refrescos, '
                               'cereales de colores, betún',
    'Miel': 'miel, miel de abeja, cereales con miel, galletas con miel',
    'Plátano': 'plátano, banana, licuado de plátano, plátano macho',
    'Cítricos': 'naranja, limón, mandarina, toronja, lima, jugo de naranja, agua de limón',
    'Jitomate': 'jitomate, tomate, salsa roja, catsup, puré de tomate, sopa de tomate, pizza',
}


def upgrade() -> None:
    table = op.create_table(
        'allergens',
        sa.Column('name', sa.String(80), nullable=False),
        sa.Column('keywords', sa.String(500), nullable=False),
        sa.Column('id', sa.UUID(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('name'),
        schema=S,
    )
    now = datetime.now(timezone.utc)
    op.bulk_insert(table, [{'id': uuid.uuid4(), 'name': n, 'keywords': k, 'created_at': now, 'updated_at': now}
                           for n, k in ALLERGENS.items()])


def downgrade() -> None:
    op.drop_table('allergens', schema=S)
